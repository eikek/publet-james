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
 * JQuery plugin to edit sieve scripts.
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 04.12.12 21:07
 */
(function ($) {
  var formTemplate =
      '<form class="sieveScriptForm form" action="{{actionUrl}}" method="post">' +
      ' {{#showLegend}}<legend>{{legendTitle}}</legend> {{/showLegend}}'+
      ' <span class="sieveFeedback"></span>'+
      ' <input type="hidden" name="lastHead"/>' +
      ' <input type="hidden" name="do" value="update"/>' +
      '{{^currentUser}}' +
      '  <div class="input-append"> ' +
      '   <input type="text" name="username" placeholder="Login" data-provide="typeahead">' +
      '   <a class="btn loadFormButton" href="#">Load</a>' +
      '  </div>' +
      ' {{/currentUser}}' +
      ' <label class="checkbox">' +
      '   <input type="checkbox" name="active"> Enable Sieve Mail Filter' +
      ' </label>' +
      ' <textarea class="scriptTextArea" name="script"></textarea>' +
      ' <div class="form-actions"> '+
      '   <button class="btn scriptSaveButton">Save</button>' +
      '   <a href="#" class="btn btn-small btn-inverse pull-right toggleEditorButton">Editor</a>' +
      ' </div>'+
      '</form>';


  function feedback($this, data) {
    var msg = $('<p/>', {
      "class": (data.success) ? "alert alert-success" : "alert alert-error"
    }).html(data.message);
    $this.find('.sieveFeedback').html(msg).animate({delay: 1}, 3500, function () {
      $this.find('.sieveFeedback').html("");
    })
  }

  function render($this, settings) {
    $this.mask();
    $this.html(Mustache.render(formTemplate, settings));
    $this.find('.toggleEditorButton').click(function(event) {
      $this.find('.scriptTextArea').codemirror('toggleEditor', {mode: "sieve"});
    });
    $this.find('.scriptSaveButton').click(function(event) {
      $this.find('.scriptTextArea').codemirror('save');
    });
    $this.find('.sieveScriptForm').ajaxForm({
      beforeSubmit: function (arr, form, options) {
        form.mask();
      },
      success: function (data, status, xhr, form) {
        form.unmask();
        form.find('input[name="lastHead"]').val(data.lastHead);
        feedback($this, data);
      }
    });
    $this.find('.scriptTextArea').codemirror({mode: "sieve"});
    if (!settings.currentUser) {
      $.get(settings.actionUrl, { "do": "getLogins" }, function(data) {
        if (data.success === false) {
          feedback($this, data);
        } else {
          $this.find('input[name="username"]').typeahead({source: data.users});
        }
      });
      $this.find('.loadFormButton').click(function(event) {
        loadFormData($this, settings);
        return false;
      });
    }
    loadFormData($this, settings);
  }

  function loadFormData($this, settings) {
    var options = { "do": "get" };
    var username = $this.find('input[name="username"]').val();
    if (username) {
      options["username"] = username;
    }
    $.get(settings.actionUrl, options, function (data) {
      $this.unmask();
      if (data.success === false) {
        feedback($this, data);
      } else {
        var form = $this.find(".sieveScriptForm");
        form.find('input[name="lastHead"]').val(data.lastHead);
        form.find('input[name="active"]').attr('checked', data.active);
        form.find('.scriptTextArea').codemirror('editor').setValue(data.script);
      }
    });
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('sieve-manager');

        if (!data) {
          var settings = $.extend({
            actionUrl: "action/managesieve.json",
            showLegend: true,
            legendTitle: 'Sieve Script',
            currentUser: false
          }, options);

          $(this).data('sieve-manager', {
            target: $this,
            settings: settings
          });

          //render form
          render($this, settings);
        }
      });
    },

    reload: function () {
      var $this = this.data('sieve-manager').target;
      var settings = this.data('sieve-manager').settings;
      reload($this, settings);
      return this;
    }
  };

  $.fn.sieveManager = function (method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jQuery.sieve-manager');
    }
  };
})(jQuery);