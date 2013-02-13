/*
 * Copyright 2013 Eike Kettner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 12.02.13 22:31
 */
(function ($) {

  var template =
      "<ul class='pager'>" +
      "{{#prevReport}}" +
      "  <li class='previous'><a href='{{prevReport}}' class='pull-left'>&larr; {{prevReportName}}</a></li>" +
      "{{/prevReport}}" +
      "  <li><a href='../report.html'>Current Report</a></li> " +
      "{{#nextReport}}" +
      "  <li class='next'><a href='{{nextReport}}' class='pull-right'>{{nextReportName}} &rarr;</a></li>" +
      "{{/nextReport}}" +
      "</div>";

  function getCurrentReport() {
    var arr = window.location.pathname.split("/");
    return arr[arr.length -1];
  }

  function feedback($this, data) {
    var msg = $('<p/>', {
      "class": (data.success) ? "alert alert-success" : "alert alert-error"
    }).html(data.message);
    $this.html(msg).animate({delay: 1}, 3500, function () {
      $this.html("");
    })
  }

  function render($this, settings) {
    $.get(settings.actionUrl, { "report": settings.report, "do": "getNext" }, function(data) {
      if (data.success === false) {
        feedback($this, data);
      } else {
        if (data.hasOwnProperty('prevReport') || data.hasOwnProperty('nextReport')) {
          $this.html(Mustache.render(template, data));
        } else {
          $this.empty();
        }
      }
    });
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('reportnav');

        if (!data) {
          var settings = $.extend({
            actionUrl: "action/reportlist.json",
            report: getCurrentReport()
          }, options);
          $(this).data('reportnav', {
            target: $this,
            settings: settings
          });

          render($this, settings);
        }
      });
    }
  };

  $.fn.reportnav = function(method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jquery.reportnav');
    }
  };
})(jQuery);