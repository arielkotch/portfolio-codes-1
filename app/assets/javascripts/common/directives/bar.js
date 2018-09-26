/**
 * A building property directive
 * changes form inputs based on property type
 * and supports multiple property types
 */
define(['angular', './main', 'highcharts'], function(angular) {
  'use strict';
  var mod = angular.module('common.directives');
  mod.directive('bar', [function() {
    return {

      restrict: 'E',
      //define the scope property, pass data through into this directive
      //isolate scope, the directive has its own scope, not the same as the scope in the html
      //get scope data off html and map it onto this scope

      scope: {
        //mapping scope into isolate scope
        //key value pairs, to give this scope data
        //passing in data and bnding data
        data: '=',
        options: '=',
        categories:'='

      },
      link: function(scope, element){
        var options = {
          chart: {
            type: 'bar',
            marginTop: 50,
            marginBottom: 70,
            height: scope.height,
            reflow: false


          },

          yAxis: {
            min: 0,
            gridLineColor: 'transparent',
            gridLineWidth: 0,
            lineWidth: 1,
            title: {
              useHTML:true,
              text: scope.options.axislabel
            }
          },
      legend:{
       align: 'right',
       verticalAlign: 'top',
       layout: 'vertical',
            itemStyle: {
                lineHeight: '14px'
            }
        },
          plotOptions: {
            series: {
              stacking: 'normal'
            },
            bar: {
              maxPointWidth: 40,
              pointPadding: 0,
              events: {
                  legendItemClick: function () {
                      return false;
                  }
              }
            }
          },
          tooltip: {
            shared: false,
            useHTML: true,
            formatter: function() {
                return '<b>' + this.x + '</b><br/>' +
                  this.series.name + ': ' + this.y + ' '+scope.options.axislabel+ '<br/>' +
                  'Total: ' + this.point.stackTotal;
            }
          },
          title: {
            text: ''
          },
          xAxis: {
            categories: scope.categories,
            labels: {
              style: {
                fontSize: '11px',
                fontWeight: 100
              },
              enabled:scope.options.label

            }
          },
          series: scope.series
        };
        var chart;
          angular.element(element).highcharts(options, function () {
             chart = this;
          });
      scope.width = window.innerWidth;
      scope.height = window.innerHeight;
      angular.element(window).bind('resize', function(){
        scope.width = window.innerWidth;
        scope.height = window.innerHeight;
        // manuall $digest required as resize event
        // is outside of angular
        scope.$digest();
      });
      scope.$watch('width',function(width){
          console.log(width,'width');
            console.log(chart,'chart');
          chart.setSize((width/2), (chart.height),true);
        });
      },
      controller: ["$scope","$element",function($scope,$element) {


        var series = [];
        var colors = ['#1F2C5C', '#3F58CE', '#5D70D4', '#08B4BB', '#6BD2D6', '#06A1F9', '#0579BB', '#F5B569', '#EB885C', '#D4483D', '#64467D', '#9A6ECE','#06AED5','#564787','#FDE74C'];
        var index;

        function createSeries() {
          index = 0;
          for (var propEnergy in $scope.data) {
            if (propEnergy !== 'net') {
              var modelEnergy = {
                name: propEnergy,
                id: propEnergy+$scope.options.id,
                data: $scope.data[propEnergy],
                color: colors[index++],
                borderWidth: 0
              };
            if($scope.options.id==='_eui'){
              modelEnergy.showInLegend=true;
               series.push(modelEnergy);
             }else{
               modelEnergy.linkedTo=propEnergy+'_eui';
               modelEnergy.showInLegend=false;
               modelEnergy.stack='_energy';
               series.push(modelEnergy);
             }
            }
          }
        }
        createSeries();
        $scope.series = series;
        console.log($element);
        $scope.height = $scope.categories.length*10+360;
        console.log($element,'el');
      }]
    };
  }]);
});
