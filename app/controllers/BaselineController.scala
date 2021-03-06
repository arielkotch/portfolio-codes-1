/**
 * Created by rimukas on 10/12/15.
 */


package controllers
import com.eclipsesource.schema._
import com.eclipsesource.schema.internal.validation.VA
import models._
import com.google.inject.Inject
import play.api.Logger
import play.api.cache.{AsyncCacheApi, SyncCacheApi}
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.Reads.min
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{Await, Future}
import squants.energy.{Energy, MegawattHours}

import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import scala.concurrent.duration._

class BaselineController @Inject() (val cache: AsyncCacheApi, cc: ControllerComponents, nrel_client: NREL_Client) extends AbstractController(cc) with Logging {

  implicit def doubleToJSValue(d: Double): JsValue = Json.toJson(d)

  implicit def energyToJSValue(b: Energy): JsValue = Json.toJson(b.value)

  implicit def listEnergyToJSValue(v: List[Energy]): JsValue = Json.toJson(v.map {
    case e: Energy => e.value
  })

   def electricityDistributionToJSValue(a: ElectricityDistribution): JsValue = {
     JsObject(Seq(
       "htg" -> JsNumber(a.elec_htg),
       "clg" -> JsNumber(a.elec_clg),
       "intLgt" -> JsNumber(a.elec_intLgt),
       "extLgt" -> JsNumber(a.elec_extLgt),
       "intEqp" -> JsNumber(a.elec_intEqp),
       "extEqp" -> JsNumber(a.elec_extEqp),
       "fans" -> JsNumber(a.elec_fans),
       "pumps" -> JsNumber(a.elec_pumps),
       "heatRej" -> JsNumber(a.elec_heatRej),
       "humid" -> JsNumber(a.elec_humid),
       "heatRec" -> JsNumber(a.elec_heatRec),
       "swh" -> JsNumber(a.elec_swh),
       "refrg" -> JsNumber(a.elec_refrg),
       "gentor" -> JsNumber(a.elec_gentor),
       "net" -> JsNumber(a.elec_net)
     ))
   }

   def ngDistributionToJSValue(a: NaturalGasDistribution): JsValue = {
     JsObject(Seq(
       "htg" -> JsNumber(a.ng_htg),
       "clg" -> JsNumber(a.ng_clg),
       "intLgt" -> JsNumber(a.ng_intLgt),
       "extLgt" -> JsNumber(a.ng_extLgt),
       "intEqp" -> JsNumber(a.ng_intEqp),
       "extEqp" -> JsNumber(a.ng_extEqp),
       "fans" -> JsNumber(a.ng_fans),
       "pumps" -> JsNumber(a.ng_pumps),
       "heatRej" -> JsNumber(a.ng_heatRej),
       "humid" -> JsNumber(a.ng_humid),
       "heatRec" -> JsNumber(a.ng_heatRec),
       "swh" -> JsNumber(a.ng_swh),
       "refrg" -> JsNumber(a.ng_refrg),
       "gentor" -> JsNumber(a.ng_gentor),
       "net" -> JsNumber(a.ng_net)
     ))
   }

   def endUseDistributionToJSValue(a: EndUseDistribution): JsValue = {
     JsObject(Seq(
       "htg" -> JsNumber(a.htg),
       "clg" -> JsNumber(a.clg),
       "intLgt" -> JsNumber(a.intLgt),
       "extLgt" -> JsNumber(a.extLgt),
       "intEqp" -> JsNumber(a.intEqp),
       "extEqp" -> JsNumber(a.extEqp),
       "fans" -> JsNumber(a.fans),
       "pumps" -> JsNumber(a.pumps),
       "heatRej" -> JsNumber(a.heatRej),
       "humid" -> JsNumber(a.humid),
       "heatRec" -> JsNumber(a.heatRec),
       "swh" -> JsNumber(a.swh),
       "refrg" -> JsNumber(a.refrg),
       "gentor" -> JsNumber(a.gentor),
       "net" -> JsNumber(a.net)
     ))
   }


  def roundAt(p: Int)(n: Double): Double = {
    val s = math pow(10, p);
    (math round n * s) / s
  }

  def apiRecover(throwable: Throwable): Either[String, JsValue] = {
    throwable match {
      case NonFatal(th) => Left(th.getMessage)
    }
  }

  def api[T](response: T): Either[String, JsValue] = {

    response match {
      case v: Energy => Right(v)
      case v: Double => Right(v)
      case v: Int => Right(Json.toJson(v))
      case v: Map[String,Any] => Right(
        JsObject(v.map {
          case (a,b) => {
            val ret: (String, JsValue) = b match {
              case _: String => a.toString -> JsString(b.asInstanceOf[String])
              case _: Double => a.toString -> JsNumber(b.asInstanceOf[Double])
              case _: Energy => a.toString -> energyToJSValue(b.asInstanceOf[Energy])
              case _: ElectricityDistribution => a.toString -> electricityDistributionToJSValue(b.asInstanceOf[ElectricityDistribution])
              case _: NaturalGasDistribution => a.toString -> ngDistributionToJSValue(b.asInstanceOf[NaturalGasDistribution])
              case _: EndUseDistribution => a.toString -> endUseDistributionToJSValue(b.asInstanceOf[EndUseDistribution])
            }
            ret
          }
        })
      )
      case v: List[Any] => Right {
        Json.toJson(v.map {
          case a: Energy => energyToJSValue(a)
          case a: ValidatedSolarMetrics => JsObject(Seq(
            "system_capacity" -> JsNumber(a.system_capacity),
            "module_type" -> JsNumber(a.module_type),
            "losses" -> JsNumber(a.losses),
            "array_type" -> JsNumber(a.array_type),
            "tilt" -> JsNumber(a.tilt),
            "azimuth" -> JsNumber(a.azimuth),
            "file_id" -> JsString(a.file_id)
          ))
          case a: ValidatedPropTypes => JsObject(Seq(
            "prop_types" -> JsString(a.building_type),
            "floor_area" -> JsNumber(a.floor_area),
            "floor_area_units" -> JsString(a.floor_area_units)
          ))
        })
        }
      case v: String => Right(Json.toJson(v))
      case None => Left("Could not recognize input type")
    }
  }



  val validator = new SchemaValidator()

  val schema = Json.fromJson[SchemaType](Json.parse(
    """{
        "type": "array",
        "id": "http://znc.maalka.com/znc",
        "items": [
          {
          "id": "/items",
          "type": "object",
          "properties": {
            "prop_types": {
               "id": "/items/properties/prop_types",
               "type": "array",
               "required":true,
               "items": {
                   "type": "object",
                   "minItems": 1,
                   "properties": {
                     "building_type": {
                     "type": "string",
                     "enum": ["OfficeLarge", "OfficeMedium", "OfficeSmall", "RetailStandalone", "RetailStripmall",
                             "SchoolPrimary", "SchoolSecondary", "Hospital", "OutPatientHealthCare",
                             "RestaurantSitDown", "RestaurantFastFood", "HotelLarge", "HotelSmall",
                             "Warehouse", "ApartmentHighRise", "ApartmentMidRise", "Office", "Retail", "School",
                             "Healthcare", "Restaurant", "Hotel", "Apartment", "Warehouse", "AllOthers"]
                     },
                     "floor_area": {
                      "type": "number",
                      "minimum": 0
                     },
                     "floor_area_units": {
                       "type": "string",
                       "enum": ["mSQ", "ftSQ"]
                     }
                   },
                   "required": [
                     "building_type",
                     "floor_area",
                     "floor_area_units"
                   ]
               }
             },
            "stories": {
              "type": "number",
              "minimum": 0,
              "required":true
            },
            "prescriptive_resource": {
              "type": ["integer","null"],
              "enum": [0,1,2,3]
            },
            "reporting_units": {
              "type": "string",
              "enum": ["imperial", "metric"]
            },
            "pv_resource": {
              "type": ["integer","null"]
            },
            "approach": {
              "type": "string",
              "enum": ["prescriptive", "performance"]
            },
            "metric": {
              "id": "/items/properties/metric",
              "type": ["object","null"],
              "properties": {
                "metric_type": {
                  "type": "string",
                  "enum": ["site"]
                },
                "conversion_resource": {
                  "type": "integer",
                  "minimum": 0
                },
                "carbon_electricity": {
                  "type": "number",
                  "minimum": 0
                },
                "carbon_natural_gas": {
                  "type": "number",
                  "minimum": 0
                },
                "carbon_fuel_oil": {
                  "type": "number",
                  "minimum": 0
                },
                "carbon_propane": {
                  "type": "number",
                  "minimum": 0
                },
                "carbon_steam": {
                  "type": "number",
                  "minimum": 0
                },
                "carbon_hot_water": {
                  "type": "number",
                  "minimum": 0
                },
                "carbon_chilled_water": {
                  "type": "number",
                  "minimum": 0
                },
                "carbon_coal": {
                  "type": "number",
                  "minimum": 0
                },
                "source_electricity": {
                  "type": "number",
                  "minimum": 0
                },
                "source_natural_gas": {
                  "type": "number",
                  "minimum": 0
                },
                "source_fuel_oil": {
                  "type": "number",
                  "minimum": 0
                },
                "source_propane": {
                  "type": "number",
                  "minimum": 0
                },
                "source_steam": {
                  "type": "number",
                  "minimum": 0
                },
                "source_hot_water": {
                  "type": "number",
                  "minimum": 0
                },
                "source_chilled_water": {
                  "type": "number",
                  "minimum": 0
                },
                "source_coal": {
                  "type": "number",
                  "minimum": 0
                }
              }
            },
            "climate_zone": {
              "type": "string",
              "enum": ["0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16"],
              "required": true
            },
            "file_id": {
              "type": "string"
            },
            "lat": {
              "type": "string"
            },
            "lon": {
              "type": "string"
            },
            "pv_data": {
              "id": "/items/properties/pv_data",
              "type": "array",
              "items": {
                "type": "object",
                "additionalProperties": false,
                "properties": {
                  "estimated_area": {
                    "minimum": 0,
                    "type": "number"
                  },
                  "pv_area_units": {
                    "minimum": 0,
                    "type": "string",
                    "enum": ["mSQ", "ftSQ"]
                  },
                  "access_perimeter": {
                    "minimum": 0,
                    "type": "number"
                  },
                  "w_per_meter2": {
                    "minimum": 0,
                    "type": "number"
                  },
                  "system_capacity": {
                    "minimum": 0.05,
                    "maximum": 10000.0,
                    "type": "number"
                  },
                  "module_type": {
                    "type": "integer",
                    "enum": [0,1,2]
                  },
                  "losses": {
                    "type": "number",
                    "minimum": -5,
                    "maximum": 99
                  },
                  "array_type": {
                    "type": "integer",
                    "enum": [0,1,2,3,4]
                  },
                  "tilt": {
                    "type": "number",
                    "minimum": 0,
                    "maximum": 90
                  },
                  "azimuth": {
                    "type": "number",
                    "minimum": 0,
                    "maximum": 360
                  },
                  "inv_eff": {
                    "type": "number",
                    "minimum": 90,
                    "maximum": 99.5
                  }
                }
              }
            },
            "energies": {
              "id": "/items/properties/energies",
              "type": ["array","null"],
              "items": {
                  "type": "object",
                  "properties": {
                    "energy_name": {
                      "type": ["string","null"]
                    },
                    "energy_type": {
                      "type": "string",
                      "enum": ["electricity","natural_gas","fuel_oil","propane","steam","hot_water","chilled_water","coal","other"]
                    },
                    "energy_units": {
                      "type": "string",
                       "enum": ["kBtu","MBtu","kWh","MWh","GJ","NG Mcf","NG kcf","NG ccf","NG cf", "NGm3","therms","Propane igal","Propane gal","Propane cf","Propane ccf",
                             "Propane kcf","Propane L","Steam lb","Steam klb","Steam Mlb","CHW TonH"]
                    },
                    "energy_use": {
                      "type": "number",
                      "minimum": 0
                    }
                  },
                  "required": [
                    "energy_type",
                    "energy_units",
                    "energy_use"
                  ]
              }
            }
          }
          }
        ]
      }""".stripMargin)).get

  def getZNCMetrics() = Action.async(parse.json) { implicit request =>

    val json: JsValue = request.body
    val result = validator.validate(schema, json)

    result.fold(
      invalid = { errors =>
        Future {
          BadRequest(errors.toJson)
        }
      },
      valid = { post =>

        val Baseline: EUIMetrics = EUIMetrics(request.body, nrel_client)

        val f1: Future[Either[String, JsValue]] = Baseline.getPV.map(api(_)).recover { case NonFatal(th) => apiRecover(th) }

        val getPV_Parameters: Future[Seq[Map[String, String]]] = f1.map { e =>
          e match {
            case Right(jsonArray: JsArray) => jsonArray.as[Seq[Map[String, JsValue]]].map {
              x => {
                x.map { y => (y._1, y._2 match {
                  case a:JsString => a.as[String]
                  case a => a.toString()
                }) }
              }}
            case _ => Seq.empty[Map[String, String]]
          }
        }

        val multipleFutures: Future[Seq[JsValue]] = getPV_Parameters.flatMap { params =>
          val a: Seq[Future[JsValue]] = params.map { p =>
            nrel_client.makeWsRequest(p.toSeq)
          }
          Future.sequence(a)
        }


        def f2(ccEnergy:Double, ccIntensity:Double) = multipleFutures.map { r =>

          val station_info = (r.head \ "station_info").get
          val version = (r.head \ "version").get
          val ac_annual: Double = r.map{a => (a \ "outputs" \ "ac_annual").as[Double]}.sum * ccEnergy
          val capacity_factor = r.map{a => (a \ "outputs" \ "capacity_factor").as[Double]}.sum / r.map{a => (a \ "outputs" \ "capacity_factor").as[Double]}.length
          val solrad_annual = r.map{a => (a \ "outputs" \ "solrad_annual").as[Double]}.sum * ccIntensity

          val arr1 =  r.map{a => (a \ "outputs" \ "ac_monthly").as[Array[Double]]}

          val ac_monthly = arr1.tail.fold(arr1.head) { (z, i) =>
            (z, i).zipped.map(_ + _)
          }.map(_ * ccEnergy)

          val arr2 =  r.map{a => (a \ "outputs" \ "dc_monthly").as[Array[Double]]}

          val dc_monthly = arr2.tail.fold(arr2.head) { (z, i) =>
            (z, i).zipped.map(_ + _)
          }.map(_ * ccEnergy)

          val arr3 =  r.map{a => (a \ "outputs" \ "poa_monthly").as[Array[Double]]}

          val poa_monthly = arr3.tail.fold(arr3.head) { (z, i) =>
            (z, i).zipped.map(_ + _).map(_ * ccIntensity)
          }

          val arr4 =  r.map{a => (a \ "outputs" \ "solrad_monthly").as[Array[Double]]}

          val solrad_monthly = arr4.tail.fold(arr4.head) { (z, i) =>
            (z, i).zipped.map(_ + _).map(_ * ccIntensity)
          }

          val a: JsValue = Json.obj(
            "outputs" -> Json.obj(
              "ac_annual" -> ac_annual,
              "capacity_factor" -> capacity_factor,
              "solrad_annual" -> solrad_annual,
              "ac_monthly" -> ac_monthly,
              "dc_monthly" -> dc_monthly,
              "poa_monthly" -> poa_monthly,
              "solrad_monthly" -> solrad_monthly
            ),
            "station_info" -> station_info,
            "version" -> version
          )
          Right(a)
        }

        def solarTotal:Future[Double] = multipleFutures.map { r =>

          r.map{a => (a \ "outputs" \ "ac_annual").as[Double]}.sum

        }


        val merged: Future[Either[String, JsValue]] = for {
          cc_energy <- Baseline.solarConversionEnergy
          cc_intensity <- Baseline.solarConversionIntensity
          e1 <- f1
          e2 <- f2(cc_energy,cc_intensity)
        } yield (e1, e2) match {
          case (Left(s: String), _) => Left(s)
//          case (_, Left(s: String)) => Left(s)
          case (Right(j1:JsValue), Right(j2:JsValue)) => {
            Right(Json.obj("PVWatts_inputs" -> j1).deepMerge(j2.as[JsObject]))
          }
        }


        val prescriptiveRequirements: Future[Map[String,Any]] = {
          for {
            cc_energy <- Baseline.solarConversionEnergy
            conversionType <- Baseline.getPrescriptiveType
            conversionConstant <- Baseline.getSolarConversionConstant(conversionType,"solar")
            totalPrescriptive <- Baseline.getPrescriptiveTotalMetric
            pvTotal <- solarTotal
            requirementsMap <- Future{
              Map(
                "re_rec_onsite_pv" -> pvTotal * conversionConstant / 1000 * cc_energy,
                "prescriptive_building_energy" -> MegawattHours(totalPrescriptive to MegawattHours).value * cc_energy,
                "prescriptive_re_total_needed" -> MegawattHours(totalPrescriptive to MegawattHours).value * cc_energy,
                "prescriptive_re_procured" -> Math.max(MegawattHours(totalPrescriptive to MegawattHours).value - (pvTotal*conversionConstant/1000),0.0) * cc_energy
              )
            }
          } yield requirementsMap
        }

        val sourceRequirements: Future[Map[String,Any]] = {
          for {
            conversionConstant <- Baseline.getSolarConversionConstant("source","solar")
            cc_energy <- Baseline.solarConversionEnergy
            totalPrescriptive <- Baseline.getPrescriptiveTotalSource
            pvTotal <- solarTotal
            requirementsMap <- Future{
              Map(
                "re_rec_onsite_pv" -> pvTotal * conversionConstant / 1000 * cc_energy,
                "prescriptive_building_energy" -> MegawattHours(totalPrescriptive to MegawattHours).value * cc_energy,
                "prescriptive_re_total_needed" -> MegawattHours(totalPrescriptive to MegawattHours).value * cc_energy,
                "prescriptive_re_procured" -> Math.max(MegawattHours(totalPrescriptive to MegawattHours).value - (pvTotal * conversionConstant /1000),0.0) * cc_energy
              )
            }
          } yield requirementsMap
        }

        val solar_inputs = multipleFutures.map { r =>

          val azimuth = (r.head \ "inputs" \ "azimuth").get
          val losses = (r.head \ "inputs" \ "losses").get
          val module_type = (r.head \ "inputs" \ "module_type").get
          val tilt = (r.head \ "inputs" \ "tilt").get
          val array_type = (r.head \ "inputs" \ "array_type").get
          val file_id = (r.head \ "inputs" \ "file_id").get
          val system_capacity = (r.head \ "inputs" \ "system_capacity").get

          val a: JsValue = Json.obj(
            "inputs" -> Json.obj(
              "azimuth" -> azimuth,
              "losses" -> losses,
              "module_type" -> module_type,
              "tilt" -> tilt,
              "array_type" -> array_type,
              "file_id" -> file_id,
              "system_capacity" -> system_capacity
            ))
          Right(a)
        }

        val solar_errors = multipleFutures.map { r =>

          val errors = r.map{a => (a \ "errors").as[Seq[String]]}.flatten

          Left(Json.toJson(errors.map{JsString(_)}).toString())
        }

        val solar_warnings = multipleFutures.map { r =>

          val warnings = r.map{a => (a \ "warnings").as[Seq[String]]}.flatten

          Left(Json.toJson(warnings.map{JsString(_)}).toString())
        }


        val futures = Future.sequence(Seq(

          prescriptiveRequirements.map(api(_)).recover { case NonFatal(th) => apiRecover(th) },
          sourceRequirements.map(api(_)).recover { case NonFatal(th) => apiRecover(th) },
          Baseline.getPrescriptiveMetrics.map(api(_)).recover { case NonFatal(th) => apiRecover(th) },

          Baseline.getPV.map(api(_)).recover { case NonFatal(th) => apiRecover(th) } ,
          Baseline.getPVarea.map(api(_)).recover { case NonFatal(th) => apiRecover(th) } ,
          Baseline.getPVcapacity.map(api(_)).recover { case NonFatal(th) => apiRecover(th) } ,

          solar_errors.recover { case NonFatal(th) => apiRecover(th) } ,
          solar_warnings.recover { case NonFatal(th) => apiRecover(th) } ,
          merged.recover { case NonFatal(th) => apiRecover(th) },

          Baseline.getBuildingData.map(api(_)).recover { case NonFatal(th) => apiRecover(th) }

        ))

        val fieldNames = Seq(

          "prescriptive_requirements",
          "source_requirements",
          "prescriptive_metrics",

          "pv_array",
          "pv_area",
          "pv_capacity_kW",
          "pvwatts_errors",
          "pvwatts_warnings",
          "pvwatts_system_details",

          "building_sub_types"

        )

        futures.map(fieldNames.zip(_)).map { r =>
          val errors = r.collect {
            case (n, Left(s)) => Json.obj(n -> s)
          }
          val results = r.collect {
            case (n, Right(s)) => Json.obj(n -> s)
          }
          Ok(Json.obj(
            "values" -> results,
            "errors" -> errors
          ))
        }
      }
    )
  }
}