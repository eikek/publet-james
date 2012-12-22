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
 * @since 21.12.12 00:43
 */
(function ($) {

  var headTempl =
    '<span class="pull-left">' +
    '  <span>Size: <span class="label sizeLabel">{{size}}</span></span> ' +
    '  <div class="btn-group">' +
    '    <a rel="tooltip" class="btn btn-mini flushBtn" title="Flush Queue"><i class="icon-envelope"></i></a>' +
    '    <a rel="tooltip" class="btn btn-mini clearBtn" title="Clear Queue"><i class="icon-trash"></i></a>' +
    '    <a rel="tooltip" class="btn btn-mini refreshBtn" title="Refresh view"><i class="icon-refresh"></i></a>' +
    '  </div>' +
    '</span>' +
    '<div class="span3 feedback"></div> '+
    '{{#allowQueueSelection}}' +
    '<span class="pull-right">' +
    '  <select class="queueSelect">{{#queueNames}}<option>{{.}}</option>{{/queueNames}}</select>' +
    '</span>' +
    '{{/allowQueueSelection}}' +
    '<div class="spoolTable"></div>';

  var spoolTableTempl =
    '<table class="table table-condensed table-striped">' +
    '<tr>' +
    '<th></th>' +
    '<th>Name</th>' +
    '<th>State</th>' +
    '<th>Sender</th>' +
    '<th>Recipients</th>' +
    '<th>Remote Host</th>' +
    '<th>Delivery</th>' +
    '</tr>' +
    '{{#mailItems}}' +
    '<tr>' +
    ' <td><a data-key="{{name}}" class="btn btn-mini clearSingle" href="#"><i class="icon-trash"></i></a></td>' +
    '  <td>{{name}}</td>' +
    '  <td>{{state}}</td>' +
    '  <td>{{sender}}</td>' +
    '  <td>{{recipients}}</td>' +
    '  <td>{{remoteHost}}</td>' +
    '  <td>{{next}}</td>' +
    '</tr>' +
    '{{/mailItems}}' +
    '</table>';


  function feedback($this, data) {
    var msg = $('<span/>', {
      "html": data.message
    });
    $this.find('.feedback').html(msg).animate({delay: 1}, 3500, function () {
      $this.find('.feedback').html("");
    });
  }

  function render($this, settings) {
    $this.mask();
    $.get(settings.actionUrl, { "do": "list", "queue": settings.defaultQueue }, function(data) {
      var view = $.extend(data, settings);
      $this.html(Mustache.render(headTempl, view)).unmask();
      if (data.success === false) {
        feedback($this, data);
      } else {
        $this.find('.queueSelect option').each(function(i, el) {
          if (el.text === data.currentQueue) {
            $(el).attr("selected", "selected");
          }
        });
        $this.find('.refreshBtn').click(function(ev) {
          render($this, settings);
        });
        $this.find('.clearBtn').click(function(ev) {
          $.post(settings.actionUrl, {"do": "clear", "queue": settings.defaultQueue}, function(data) {
            feedback($this, data);
            reload($this, settings);
          });
        });
        $this.find('.flushBtn').click(function(ev) {
          $.post(settings.actionUrl, {"do": "flush", "queue": settings.defaultQueue}, function(data) {
            feedback($this, data);
            reload($this, settings);
          });
        });
        $this.find('.queueSelect').change(function(ev) {
          settings.defaultQueue = $(ev.currentTarget).val();
          render($this, settings);
        });
        renderTable($this, settings, data);
      }
    });
  }

  function renderTable($this, settings, data) {
    var view = $.extend(data, settings);
    $this.find('.spoolTable').html(Mustache.render(spoolTableTempl, view));
    $this.find('.sizeLabel').html(data.size);
    $this.find('.clearSingle').click(function(ev) {
      var key = $(ev.currentTarget).attr("data-key");
      $.post(settings.actionUrl, {"do": "remove", "queue": settings.defaultQueue, "name": key }, function(data) {
        feedback($this, data);
        reload($this, settings);
      });
    });
  }

  function reload($this, settings) {
    $.get(settings.actionUrl, { "do": "list", "queue": settings.defaultQueue }, function(data) {
      if (data.success === false) {
        feedback($this, data);
      } else {
        renderTable($this, settings, data);
      }
    });
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('spool-manager');

        if (!data) {
          var settings = $.extend({
            actionUrl: "action/managespool.json",
            defaultQueue: "spool",
            allowQueueSelection: true
          }, options);
          $(this).data('spool-manager', {
            target: $this,
            settings: settings
          });

          render($this, settings);
        }
      });
    },

    setQueue: function(name) {
      var settings = this.data('spool-manager').settings;
      settings.defaultQueue = name;
      render(this, settings);
      return this;
    },

    updateView: function() {
      var settings = this.data('spool-manager').settings;
      reload(this, settings);
      return this;
    }

  };

  $.fn.spoolManager = function(method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jQuery.jquery.spool-manager.js');
    }
  };
})(jQuery);