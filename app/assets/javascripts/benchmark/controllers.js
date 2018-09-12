/*global
    maalkaIncludeHeader
*/
/**
 * Dashboard controllers.
 */
//define(["./test/sample_response_test_data"], function(sampleTestData) {
define(['angular'], function() {
  'use strict';
  var RootCtrl = function($rootScope) { 
    $rootScope.includeHeader = maalkaIncludeHeader;
  };


  RootCtrl.$inject = ['$rootScope'];

  var DashboardCtrl = function($rootScope, $scope, $window, $sce, $timeout, $q, $log, benchmarkServices) {



    $rootScope.includeHeader = maalkaIncludeHeader;
    $rootScope.pageTitle = "Portfolio Codes";


    $scope.auxModel = {};

    $scope.auxModel.climate_zone = null;
    $scope.auxModel.reporting_units = "imperial";
    $scope.temp = {};
    $scope.tempModel = {};

    $scope.benchmarkResult = null;

    $scope.propOutputList = [];
    $scope.forms = {'hasValidated': false};
    $scope.propTypes = [];
    $scope.mainColumnWidth = "";
    $scope.propText = "Primary Building Use";

    $scope.csvData = {};



    if (window.matchMedia) {

        var printQueryList = window.matchMedia('print');
        var phoneQueryList = window.matchMedia('(max-width: 767px)');      
        var tabletQueryList = window.matchMedia('(min-width: 768px) and (max-width: 1200px)');            
        var desktopQueryList = window.matchMedia('(min-width: 1200px) and (max-width: 1919px');                  
        var largeQueryList = window.matchMedia('(min-width: 1919px)');                  

        var updateMatchMedia= function () {
            //console.log(q);
            if (printQueryList.matches) {
                $scope.media = "print";                                    
            } else if (phoneQueryList.matches) {
                $scope.media = "phone";                        
            } else if (tabletQueryList.matches) { 
                $scope.media = "tablet";            
            } else if (desktopQueryList.matches) { 
                $scope.media = "desktop";
            } 

            if (largeQueryList.matches) {
                $scope.largeScreen = true;
            } else {
                $scope.largeScreen = false;
            }

           // console.log($scope.media);
            $timeout(function () {
                $scope.$apply();
            });
        };
        updateMatchMedia();

        printQueryList.addListener(updateMatchMedia);

        $scope.$on("$destroy", function handler() {
            printQueryList.removeListener(updateMatchMedia);
        });
    }

    function add(a, b) {
        return a + b;
    }


    $scope.$watch("tempModel.buildingType", function (v) {
        if (v === undefined || v === null) {
            return; 
        }

        if(v){

            $scope.propTypes.push({
                changeTo: v,
                type: v.id,
                name: v.name,
            });

            $scope.propText="Add Another Building";
            $scope.tempModel.resetBuildingType = true;
            $scope.tempModel.buildingType = undefined;
        }
    });

    $scope.updatePropType = function($index) {

        $scope.propTypes[$index] = {
            changeTo: $scope.propTypes[$index].changeTo,
            type: $scope.propTypes[$index].changeTo.id,
            name: $scope.propTypes[$index].changeTo.name,
        };
    };


    $scope.$watch("auxModel.approach", function () {

        $scope.forms.hasValidated = false;
        $scope.benchmarkResult = null;

    });



    $scope.print = function () {
        window.print();
    };


    $scope.removeProp = function(prop){
        var index;
        for(var i = 0; i < $scope.propTypes.length; i++ ) {
            if($scope.propTypes[i].name === prop.model.name) {
                index = i;
                break;
            }
        }
        $scope.propTypes.splice(index, 1);
    };

    $scope.showDivider = function() {
        if($scope.pvList.length > 1){
            return true;
            }else {
            return false;
                }
    };

    $scope.clearProp = function(prop){
        var index;

        for(var i = 0; i < $scope.propTypes.length; i++ ) {
            if($scope.propTypes[i].name === prop.name) {
                index = i;
                break;
            }
        }

        $scope.propTypes.splice(index, 1);
        if($scope.propTypes.length === 0){
            $scope.propText="Primary Building Use";
        }
    };



    $scope.computeBenchmarkResult = function(submission){

        $log.info(submission);

        $scope.futures = benchmarkServices.getEnergyMetrics(submission);

        $q.resolve($scope.futures).then(function (results) {

            console.log(results);

            $scope.endUses = results;

        });
    };

    $scope.submitErrors = function () {
        if($scope.forms.baselineForm.$error.required){
            for (var i = 0; i < $scope.forms.baselineForm.$error.required.length; i++){
                $log.info($scope.forms.baselineForm.$error.required[i].$name);
            }
            $scope.performanceError = false;
        } else {
            $scope.performanceError = true;
        }
    };

    $scope.submit = function () {

        if($scope.auxModel.approach === "manual"){
            $scope.submitForm();
        } else {
            $scope.submitCSV();
        }
    };


    $scope.submitCSV = function () {

            if ($scope.csvData) {
                var csvJSON = [];
                csvJSON.push($scope.csvData.sites);
                console.log(csvJSON);
                $scope.computeBenchmarkResult(csvJSON);

            } else {
                console.log("No CSV Processed.");
            }

    };

    $scope.submitForm = function () {

        $scope.endUses = null;

        if($scope.forms.baselineForm === undefined) {
            return;
        }

        $scope.forms.hasValidated = true; /// only check the field errors if this form has attempted to validate.


        var getPropTypes = function () {

            var props = [];
            for (var i =0; i < $scope.propTypes.length; i ++) {
                props.push($scope.propTypes[i].propertyModel);
                }
            return props;
        };



        if($scope.forms.baselineForm.$valid){

            $scope.submitArray = [];
            $scope.auxModel.prop_types = getPropTypes();
            $scope.submitArray.push($scope.auxModel);

            $scope.computeBenchmarkResult($scope.submitArray);

        }else {
            $scope.displayErrors();
        }

    };

    $scope.getPropResponseField = function(propResponse,key){
        var returnValue;

        for (var i =0; i < propResponse.values.length; i ++) {
            if (propResponse.values[i][key] !== undefined) {
              returnValue = propResponse.values[i][key];
              break;
            }
        }
        return returnValue;
    };

    $scope.getPropSize = function(building_sub_types){

        var size = [];
        for (var i =0; i < building_sub_types.length; i ++) {
            size.push(building_sub_types[i].floor_area);
        }

        return size.reduce(add, 0);
    };


    $scope.computeEndUses = function(){

        console.log("Will compute here");

//        function nullZero(a) {
//            if(a===0){
//                return null;
//            } else {
//            return a;
//            }
//        }


//        var endUses = ["Heating", "Cooling", "Interior Lighting", "Plug Loads", "Service Hot Water", "Fans"];
//        var shortNames = ["htg", "clg", "intLgt", "intEqp", "swh", "fans"];
//        var othersEndUses = ["Exterior Equipment","Exterior Light","Generators","Heat Recovery","Heat Rejection","Humidity Control","Pumps","Refrigeration"];
//        var othersNames = ["extEqp","extLgt","gentor","heatRec","heatRej","humid","pumps","refrg"];
//
//
//        var endUsesTable = {} ;
//        endUsesTable.endUses = [] ;
//        endUsesTable.endUsesOther = [] ;
//        endUsesTable.endUsesTotal = [] ;
//
//
//        var endUseResponse = $scope.getPropResponseField(results,"prescriptive_metrics");
//
//        endUsesTable.eui = endUseResponse.site_eui;
//        endUsesTable.energy = endUseResponse.site_energy / 1000; // this will be either MBtu or MWh
//
//
//
//        for (var i =0; i < endUses.length; i ++) {
//
//            endUsesTable.endUses.push([
//                endUses[i],
//                nullZero(endUseResponse.prescriptive_electricity_metric_data[shortNames[i]]),
//                nullZero(endUseResponse.prescriptive_natural_gas_metric_data[shortNames[i]]),
//                nullZero(endUseResponse.prescriptive_end_use_metric_data[shortNames[i]]),
//                endUseResponse.prescriptive_end_use_metric_percents[shortNames[i]]*100
//            ]);
//        }


    };


    $scope.displayErrors = function () {
        $scope.submitErrors();
        $scope.benchmarkResult = null;
    };

    $scope.buildingProperties = {

        buildingType: {
            commercial: [
                {id:"Office",name:"Office"},
                {id:"Retail",name:"Retail"},
                {id:"School",name:"School"},
                {id:"Restaurant",name:"Restaurant"},
                {id:"Hotel",name:"Hotel"},
                {id:"Warehouse",name:"Warehouse"},
                {id:"Apartment",name:"Apartment"}
            ]
        }
    };

    $scope.geographicProperties = {
        country : [],
        city : [],
        climate_info : [
            {id:"1",file_id:"0-24283"},
            {id:"2",file_id:"1-724957"},
            {id:"3",file_id:"1-724930"},
            {id:"4",file_id:"1-724945"},
            {id:"5",file_id:"1-723940"},
            {id:"6",file_id:"1-722956"},
            {id:"7",file_id:"1-722900"},
            {id:"8",file_id:"1-722976"},
            {id:"9",file_id:"1-722880"},
            {id:"10",file_id:"1-722869"},
            {id:"11",file_id:"1-725910"},
            {id:"12",file_id:"1-724839"},
            {id:"13",file_id:"0-93193"},
            {id:"14",file_id:"1-723820"},
            {id:"15",file_id:"1-722868"},
            {id:"16",file_id:"1-725845"}
        ]
    };



  };
  DashboardCtrl.$inject = ['$rootScope', '$scope', '$window','$sce','$timeout', '$q', '$log', 'benchmarkServices'];
  return {
    DashboardCtrl: DashboardCtrl,
    RootCtrl: RootCtrl    

  };
});
