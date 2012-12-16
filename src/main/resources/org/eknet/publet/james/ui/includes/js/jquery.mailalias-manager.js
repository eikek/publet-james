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
 * @since 16.12.12 13:51
 */
(function ($) {
  var formTemplate =
      '<div class="well"><form class="form form-inline mappingAddForm" method="post" action="{{actionUrl}}">' +
          ' {{#showLegend}}<legend>{{legendTitle}}</legend> {{/showLegend}}'+
          '<div class="control-group">' +
          '  <input type="hidden" name="do" value="add"/>'+
          '  <div class="controls">' +
          '    <input type="text" name="user" placeholder="user"/> @' +
          '    <select name="domain"><option>localhost</option></select> '+
          '    <button type="submit" class="btn mappingAddButton">Add</a>' +
          '  </div>' +
          '</div>' +
          '<div class="mappingFeedback"></div>'+
          '</form></div>' +
          '<div class="mappingList"></div>';


  var mappingListTemplate =
      '<ul class="editableList">' +
      '{{#mappings}}' +
      '  <li>' +
      '    <a class="btn btn-mini mappingRemoveButton"><i class="icon-trash"></i></a>' +
      '    <span>{{.}}</span> ' +
      '  </li>' +
      '{{/mappings}}' +
      '</ul>';

  function feedback($this, data) {
    var msg = $('<p/>', {
      "class": (data.success) ? "alert alert-success" : "alert alert-error"
    }).html(data.message);
    $this.find('.mappingFeedback').html(msg).animate({delay: 1}, 3500, function () {
      $this.find('.mappingFeedback').html("");
    })
  }

  function reloadAliasList($this, settings) {
    $this.mask();
    $.get(settings.actionUrl, { "do": "getalias" }, function(data) {
      $this.unmask();
      if (data.success === false) {
        feedback($this, data)
      } else {
        var view = { mappings: data };
        $this.find('.mappingList').html(Mustache.render(mappingListTemplate, view));
        $this.find('.mappingRemoveButton').click(function (ev) {
          var mapping = $(ev.currentTarget).siblings("span").text();
          if (mapping) {
            $.post(settings.actionUrl, {"do":"delete", "alias": mapping }, function(data) {
              feedback($this, data);
              if (data.success) {
                reloadAliasList($this, settings);
              }
            });
          }
        });
      }
    });
  }

  function render($this, settings) {
    $this.html(Mustache.render(formTemplate, settings));
    $.get(settings.domainActionUrl, { "do": "get" }, function(data) {
      var optionsTempl = "{{#domain}}<option>{{.}}</option>{{/domain}}";
      var options = Mustache.render(optionsTempl, {domain: data});
      $this.find('select[name="domain"]').html(options);
    });

    $this.find('.mappingAddForm').ajaxForm({
      beforeSubmit: function (arr, form, options) {
        form.mask();
      },
      success: function (data, status, xhr, form) {
        if (data.success) {
          form.find('[name="user"]').clearFields();
          reloadAliasList($this, settings);
        }
        form.unmask();
        feedback($this, data);
      }
    });
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('mailalias-manager');

        if (!data) {
          var settings = $.extend({
            actionUrl: "action/managemailalias.json",
            domainActionUrl: "action/managedomains.json",
            showLegend: true,
            legendTitle: 'Mail Alias'
          }, options);
          $(this).data('mailalias-manager', {
            target: $this,
            settings: settings
          });

          render($this, settings);
          reloadAliasList($this, settings);
        }
      });
    }
  };

  $.fn.mailAliasManager = function(method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jQuery.jquery.mailalias-manager.js');
    }
  };
})(jQuery);