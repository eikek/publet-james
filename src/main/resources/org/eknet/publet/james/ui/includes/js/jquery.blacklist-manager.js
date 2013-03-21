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
 * JQuery plugin that is the client frontend for the server side script `ManageBlacklist.scala`.
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 21.03.13 21:07
 */
(function ($) {
  var formTemplate =
      '<div class="well"><form id="blacklistAddForm" class="form form-inline" action="{{actionUrl}}" method="post">' +
          ' {{#showLegend}}<legend>{{legendTitle}}</legend> {{/showLegend}}'+
          ' <input type="hidden" name="{{actionParam}}" value="add"/>' +
          ' <div class="input-append">' +
          '  <input type="text" placeholder="Add" name="ip"/>' +
          '  <button class="btn">Add</button>' +
          ' </div>' +
          '</form></div>' +
          '<div class="blacklistFeedback"></div>' +
          '<div class="blacklist"></div>';

  var blacklistTemplate =
      '<ul class="editableList">' +
          '  {{#ips}}' +
          '  <li>' +
          '    <a data-ip="{{.}}" class="btn btn-mini blacklistRemoveButton"><i class="icon-trash"></i></a>' +
          '    {{.}}' +
          '  </li>' +
          '  {{/ips}}' +
          '</ul>';

  function feedback($this, data) {
    var msg = $('<p/>', {
      "class": (data.success) ? "alert alert-success" : "alert alert-error"
    }).html(data.message);
    $this.find('.blacklistFeedback').html(msg).animate({delay: 1}, 3500, function () {
      $this.find('.blacklistFeedback').html("");
    })
  }

  function addRemoveHandlers($this, settings) {
    $this.find('.blacklistRemoveButton').click(function (event) {
      var ip = $(event.target).attr("data-ip") || $(event.target.parentElement).attr("data-ip");
      if (ip) {
        $(event.target).mask();
        $.get(settings.actionUrl, { "do": "remove", "ip": ip }, function (result) {
          $(event.target).unmask();
          feedback($this, result);
          reload($this, settings);
        });
      }
    });
  }

  function reload($this, settings) {
    var target = $this.find('.blacklist');
    target.mask();
    $.get(settings.actionUrl, { "do": "get"}, function (data) {
      target.empty().unmask();
      if (data.success === false) {
        feedback($this, data);
      } else {
        target.html(Mustache.render(blacklistTemplate, data));
        addRemoveHandlers($this, settings);
      }
    });
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('blacklist-manager');

        if (!data) {
          var settings = $.extend({
            actionUrl: "action/manageblacklist.json",
            showLegend: true,
            legendTitle: 'Blacklist'
          }, options);

          $(this).data('blacklist-manager', {
            target: $this,
            settings: settings
          });

          //render form
          var view = $.extend({
            actionParam: "do"
          }, settings);
          $this.append(Mustache.render(formTemplate, view));
          $this.find('#blacklistAddForm').ajaxForm({
            beforeSubmit: function (arr, form, options) {
              form.mask();
            },
            success: function (data, status, xhr, form) {
              form.unmask().clearForm();
              reload($this, settings);
              feedback($this, data);
            }
          });

          //initial load of domain list
          reload($this, settings);
        }
      });
    },

    reload: function () {
      var $this = $(this).data('blacklist-manager').target;
      var settings = $(this).data('blacklist-manager').settings;
      reload($this, settings);
      return this;
    }
  };

  $.fn.blacklistManager = function (method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jQuery.blacklist-manager');
    }
  };
})(jQuery);