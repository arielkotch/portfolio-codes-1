/**
 * A benchmark cell directive
 * prints out the value or an error based on the response from the benchmark web server
 * that references them all.
 */
define(['angular','./main'], function(angular) {
  'use strict';

  var mod = angular.module('common.directives');
  mod.directive('energyResult', [function() {

    function add(a, b) {
        return a + b;
    }

    return {
      restrict: 'A',
      scope: {
        'result': "=",
        'units' : "="
      },
      templateUrl: 'javascripts/common/partials/energy_result.html',
      link: function(scope) {


        scope.$watch('result', function(result) {
          if (result === undefined || result === null) {
            return;
          }

          if (result !== undefined){

            var months = ["January","February","March","April","May","June","July","August","September","October","November","December"];

                    var solarTable = {} ;
                    solarTable.monthly = [] ;

                    var solarResults = result.outputs;

                    solarTable.file_id = result.station_info.solar_resource_file;
                    solarTable.city = result.station_info.city;
                    solarTable.state = result.station_info.state;

                    solarTable.dc_hours = solarResults.dc_monthly.reduce(add, 0);
                    solarTable.ac_hours = solarResults.ac_monthly.reduce(add, 0);

                    for (var i =0; i < solarResults.ac_monthly.length; i ++) {
                        solarTable.monthly.push([months[i],solarResults.ac_monthly[i],solarResults.dc_monthly[i]]);
                    }


                    solarTable.total = ["Total",solarTable.ac_hours,solarTable.dc_hours];
                    scope.solarResults = solarTable;

          }
          scope.tableEnergyUnits = scope.units;

        });
      }
    };
  }]);
  return mod;
});
