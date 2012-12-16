/*
 * Copyright 2012 Eike Kettner
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
 * @since 16.12.12 02:24
 */
(function ($) {
  var boxTemplate =
      '<ul class="serverBoxes">' +
      '<li>' +
      '  <div class="serverType">fetchmail</div> ' +
      '  <div class="left">' +
      '    <span class="label label-{{startedLabel}}">' +
      '      {{schedulerState}}' +
      '    </span><br/> ' +
      '    interval <span class="badge badge-info">{{interval}}</span>' +
      '  </div> ' +
      '  <div class="right">' +
      '    <a class="btn btn-primary btn-small" data-action="{{action}}">' +
      '      <i class="icon-{{action}} icon-white"></i>' +
      '    </a>' +
      '    <a class="btn btn-small widgetRefresh"><i class="icon-refresh"></i></a>' +
      '  </div>' +
      '</li>' +
      '</ul>';

  function reload($this, settings) {
    $this.mask();
    $.get(settings.actionUrl, { "do": "get"}, function(data) {
      $this.unmask().html(Mustache.render(boxTemplate, data));

      $this.find('.widgetRefresh').on('click', function(ev) {
        reload($this, settings);
      });
      $this.find('a[data-action]').on('click', function(event) {
        $this.mask();
        $.post(settings.actionUrl, {"do": $(event.currentTarget).attr("data-action")}, function(data) {
          reload($this, settings);
        });
      });
    });
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('fetchmail-scheduler-manager');

        if (!data) {
          var settings = $.extend({
            actionUrl: "action/managefetchmailscheduler.json"
          }, options);
          $(this).data('fetchmail-scheduler-manager', {
            target: $this,
            settings: settings
          });

          reload($this, settings);
        }
      });
    }
  };

  $.fn.fetchmailSchedulerManager = function(method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jQuery.jquery.fetchmailscheduler-manager.js');
    }
  };
})(jQuery);