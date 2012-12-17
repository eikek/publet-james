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
 * JQuery plugin for managing mappings. This is the client frontend for the
 * server side script `ManageMappings.scala`.
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 05.12.12 18:33
 */
(function ($) {
  var formTemplate =
      '<div class="well"><form class="form form-inline mappingAddForm" method="post" action="{{actionUrl}}">' +
          ' {{#showLegend}}<legend>{{legendTitle}}</legend> {{/showLegend}}'+
          '<div class="control-group">' +
          '  <input type="hidden" name="{{actionParam}}" value=""/>'+
          '  <div class="controls">' +
          '    <input type="text" name="user" placeholder="user"/> @' +
          '    <input type="text" name="domain" placeholder="domain"/>' +
          '  </div>' +
          '</div>' +
          '<div class="control-group">' +
          '  <div class="controls input-append">' +
          '     <input type="text" name="mapping" placeholder="mapping"/>' +
          '     <a class="btn mappingAddButton">Add</a>' +
          '     <a class="btn singleMappingRemoveButton"">Remove</a>' +
          '     <button class="btn" type="reset">Reset</button>' +
          '  </div>' +
          '</div>' +
          '<div class="mappingFeedback"></div>'+
          '</form></div>' +
          '<div><form class="form-search mappingSearchForm" action="{{actionUrl}}" method="post">' +
          '  <div class="input-append">' +
          '    <input type="text" class="input-medium search-query" placeholder="Search..." name="q">' +
          '    <input type="hidden" name="do" value="get"/>'+
          '    <button class="btn">Search</button>' +
          '  </div>' +
          '</form>' +
          '</div>' +
          '<div class="mappingList"></div>';


  var mappingListTemplate =
      '<ul class="editableList">' +
          '{{#mappings}}' +
          '  <li data-userdomain="{{mapping}}">' +
          '    <a class="btn btn-mini mappingRemoveButton"><i class="icon-trash"></i></a>' +
          '    <strong>{{mapping}}</strong>: ' +
          '    {{#targets}}' +
          '      <a href="#" class="mappingEditButton" data-mapping="{{.}}">{{.}}</a> ' +
          '    {{/targets}}' +
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

  function addEditHandler($this) {
    $this.find('.mappingEditButton').on('click', function(event) {
      var mapping = $(event.target).attr("data-mapping");
      var userdomain = $(event.target).parents("[data-userdomain]").attr("data-userdomain").split("@");
      var user = userdomain[0];
      var domain = userdomain[1];
      var form = $this.find('.mappingAddForm');
      form.find('input[name="user"]').val(user);
      form.find('input[name="domain"]').val(domain);
      form.find('input[name="mapping"]').val(mapping);
      return false;
    })
  }

  function addDeleteHandler($this, settings) {
    $this.find('.mappingRemoveButton').on('click', function(event) {
      var userdomain = $(event.target).parents("[data-userdomain]").attr("data-userdomain").split("@");
      var user = userdomain[0];
      var domain = userdomain[1];
      $this.mask();
      $.post(settings.actionUrl, { "do": "remove", "user": user, "domain": domain}, function(message) {
        $this.unmask().clearForm();
        reload($this, settings);
        feedback($this, message);
      });
    });
  }

  function reload($this, settings, query) {
    var target = $this.find('.mappingList');
    target.empty().mask();
    var options = { "do": "get" };
    if (query) {
      options["q"] = query;
    }
    $.get(settings.actionUrl, options, function(data) {
      target.unmask();
      if (data.success === false) {
        feedback($this, data);
      } else {
        //get map[string, list[string]]. must create something that can work with mustache
        var view = [];
        for (var key in data) {
          if (data.hasOwnProperty(key)) {
            view.push({ mapping: key, targets: data[key] });
          }
        }
        target.html(Mustache.render(mappingListTemplate, {mappings: view}));
        addEditHandler($this);
        addDeleteHandler($this, settings);
      }
    });
  }
  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('mapping-manager');

        if (!data) {
          var settings = $.extend({
            actionUrl: "action/managemappings.json",
            showLegend: true,
            legendTitle: "Mappings"
          }, options);
          $(this).data('mapping-manager', {
            target: $this,
            settings: settings
          });

          var view = $.extend({ actionParam: "do" }, settings);
          $this.append(Mustache.render(formTemplate, view));
          var submitForm = function() {
            $this.find('.mappingAddForm').ajaxSubmit({
              beforeSubmit: function (arr, form, options) {
                form.mask();
              },
              success: function (data, status, xhr, form) {
                form.unmask().clearForm();
                reload($this, settings);
                feedback($this, data);
              }
            });
          };
          $this.find('.mappingAddButton').on('click', function(event) {
            $this.find('input[name="do"]').val("add");
            submitForm();
          });
          $this.find('.singleMappingRemoveButton').on('click', function(event) {
            $this.find('input[name="do"]').val("remove");
            submitForm();
          });
          $this.find('.mappingSearchForm').submit(function() {
            var query = $(this).find('input[name="q"]').val();
            reload($this, settings, query);
            return false;
          });
          reload($this, settings);
        }
      });
    },

    reload: function () {
      var $this = $(this).data('mapping-manager').target;
      var settings = $(this).data('mapping-manager').settings;
      reload($this, settings);
      return this;
    }

  };
  $.fn.mappingManager = function (method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jQuery.mapping-manager.js');
    }
  };
})(jQuery);