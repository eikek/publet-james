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
 * JQuery plugin that is the client frontend for the server side script `ManageDomains.scala`.
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 04.12.12 21:07
 */
(function ($) {
  var formTemplate =
      '<div class="well"><form id="domainAddForm" class="form form-inline" action="{{actionUrl}}" method="post">' +
          ' {{#showLegend}}<legend>{{legendTitle}}</legend> {{/showLegend}}'+
          ' <input type="hidden" name="{{actionParam}}" value="add"/>' +
          ' <div class="input-append">' +
          '  <input type="text" placeholder="Add Domain" name="domain"/>' +
          '  <button class="btn">Add</button>' +
          ' </div>' +
          '</form></div>' +
          '<div class="domainFeedback"></div>' +
          '<div class="domainList"></div>';

  var domainListTemplate =
      '<ul class="editableList">' +
          '  {{#domains}}' +
          '  <li {{#defaultEntry}}class="defaultEntry"{{/defaultEntry}}>' +
          '    <a data-domain="{{name}}" class="btn btn-mini {{^defaultEntry}}domainRemoveButton{{/defaultEntry}}" {{#defaultEntry}}disabled="disabled"{{/defaultEntry}}><i class="icon-trash"></i></a>' +
          '    {{name}}' +
          '  </li>' +
          '  {{/domains}}' +
          '</ul>';

  function feedback($this, data) {
    var msg = $('<p/>', {
      "class": (data.success) ? "alert alert-success" : "alert alert-error"
    }).html(data.message);
    $this.find('.domainFeedback').html(msg).animate({delay: 1}, 3500, function () {
      $this.find('.domainFeedback').html("");
    })
  }

  function addRemoveHandlers($this, settings) {
    $this.find('.domainRemoveButton').click(function (event) {
      var disabled = $(event.target).attr("disabled") || $(event.target.parentElement).attr("disabled");
      if (!disabled) {
        var domain = $(event.target).attr("data-domain") || $(event.target.parentElement).attr("data-domain");
        if (domain) {
          $(event.target).mask();
          $.get(settings.actionUrl, { "do": "remove", "domain": domain}, function (result) {
            $(event.target).unmask();
            feedback($this, result);
            reload($this, settings);
          });
        }
      }
    });
  }

  function reload($this, settings) {
    var target = $this.find('.domainList');
    target.mask();
    $.get(settings.actionUrl, { "do": "get"}, function (data) {
      $.get(settings.actionUrl, { "do": "getdefault"}, function (defaultDomain) {
        target.empty().unmask();
        var view = { domains: [] };
        for (var i = 0; i < data.length; i++) {
          view.domains.push({
            name: data[i],
            defaultEntry: data[i] === defaultDomain
          });
        }
        target.html(Mustache.render(domainListTemplate, view));
        addRemoveHandlers($this, settings);
      });
    });
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('domain-manager');

        if (!data) {
          var settings = $.extend({
            actionUrl: "action/managedomains.json",
            showLegend: true,
            legendTitle: 'Domains'
          }, options);

          $(this).data('domain-manager', {
            target: $this,
            settings: settings
          });

          //render form
          var view = $.extend({
            actionParam: "do"
          }, settings);
          $this.append(Mustache.render(formTemplate, view));
          $this.find('#domainAddForm').ajaxForm({
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
      var $this = $(this).data('domain-manager').target;
      var settings = $(this).data('domain-manager').settings;
      reload($this, settings);
      return this;
    }
  };

  $.fn.domainManager = function (method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jQuery.domain-manager');
    }
  };
})(jQuery);