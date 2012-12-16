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
 * @since 15.12.12 22:48
 */
(function ($) {

  var addFormTemplate =
      '<div class="modal hide fade fetchmailAccountModal">\n' +
      '  <form class="updateAccountForm" action="{{actionUrl}}" method="post">\n' +
      '    <div class="modal-header">\n' +
      '      <button class="close" data-dismiss="modal" aria-hidden="true">&times;</button>\n' +
      '      <h3>Fetchmail Account</h3>\n' +
      '    </div>\n'+
      '    <div class="modal-body">\n' +
      '      {{^login}}' +
      '      <label>Local user (login)</label>\n' +
      '      <input type="text" placeholder="login" name="login" required="required"/>\n' +
      '      {{/login}}' +
      '      <label>Remote user</label>\n' +
      '      <input type="text" placeholder="remote user" name="user" required="required"/>\n' +
      '      <label>Pop3 Host</label>\n' +
      '      <input type="text" placeholder="remote pop3 host" name="host" required="required"/>\n' +
      '      <label>Password</label>\n' +
      '      <input type="password" placeholder="remote password" name="password" />\n' +
      '      <label>Update interval (1-10)</label>\n' +
      '      <input type="text" placeholder="update interval" name="runInterval" />\n' +
      '      <label class="checkbox">\n' +
      '        <input type="checkbox" name="active"> Active</input>\n' +
      '      </label>\n' +
      '    </div>\n' +
      '    <div class="modal-footer">\n' +
      '      <input type="hidden" name="do" value="update"/>' +
      '      <span class="feedback"></span>' +
      '      <button class="btn btn-primary">Save</button>\n' +
      '      <button class="btn" type="reset">Reset</button>\n' +
      '    </div>\n'+
      '  </form>\n'+
      '</div>';

  var listFormTemplate =
      '<form class="form-inline listAccountForm" action="{{actionUrl}}" method="post">' +
      '  <input type="hidden" name="do" value="get"/>\n ' +
      '  <div class="input-append">\n' +
      '      {{^login}}' +
      '    <input type="text" placeholder="Login" name="login" />\n' +
      '    <button class="btn listAccountsButton">List</button>\n' +
      '      {{/login}}' +
      '  </div>\n' +
      '  <a class="btn newAccountButton" href="#" role="button" data-toggle="modal">New</a>\n' +
      '  <span class="feedback"></span>' +
      '</form>\n';

  var tableTemplate =
      '<table class="table table-condensed tabel-striped">\n' +
      '  <tr><th>Active</th><th>User</th><th>Host</th><th>Interval</th><th></th></tr>' +
      '{{#accounts}} \n' +
      '  <tr data-login="{{login}}">\n' +
      '    <td data-name="active"><input type="checkbox" {{#active}}checked="yes"{{/active}} disabled="disabled"> </td>\n' +
      '    <td data-name="user">{{user}}</td>\n' +
      '    <td data-name="host">{{host}}</td>\n' +
      '    <td data-name="runInterval">{{runInterval}}</td>\n' +
      '    <td><a class="btn btn-mini editAccountButton"><i class="icon-pencil"></i></a>\n' +
      '        <a class="btn btn-mini deleteAccountButton"><i class="icon-trash"></i></a></td>\n' +
      '  </tr>\n' +
      '{{/accounts}}' +
      '</table>';

  function feedback(el, data) {
    var level = (data.success)? "success" : "error";
    el.addClass("alert");
    el.addClass("alert-"+level);
    el.html(data.message).animate({delay: 1}, 3500, function () {
      el.html("");
      el.removeClass("alert");
      el.removeClass("alert-"+level);
    })
  }

  function addHandlers($this, settings) {
    //list accounts
    var options = {
      beforeSubmit: function (arr, form, options) {
        form.mask();
      },
      success: function (data, status, xhr, form) {
        form.unmask();
        var view = { accounts: data };
        $this.find('.resultTable').html(Mustache.render(tableTemplate, view));

        addTableHandlers($this, settings);
      }
    };
    $this.find('.listAccountForm').ajaxForm(options);

    //new account button
    $this.find(".newAccountButton").on('click', function(ev) {
      $this.find('.fetchmailAccountModal').modal('show');
      $this.find('.updateAccountForm').clearForm();
    });

    //new account form
    $this.find('.updateAccountForm').ajaxForm({
      beforeSubmit: function (arr, form, options) {
        form.mask();
      },
      success: function (data, status, xhr, form) {
        form.unmask();
        if (data.success) {
          $this.find('.fetchmailAccountModal').modal('hide');
          reloadAccounts($this);
        } else {
          feedback(form.find(".feedback"), data)
        }
      }
    })
  }

  function addTableHandlers($this, settings) {
    //edit account button
    $this.find('.editAccountButton').on('click', function(ev) {
      $this.find('.fetchmailAccountModal').modal('show');
      var target = $(ev.currentTarget);
      var tr = target.parents("tr");
      var form = $this.find('.updateAccountForm').clearForm();
      form.find('[name="login"]').val(tr.attr("data-login"));
      tr.children('[data-name]').each(function(index, el) {
        var child = $(el);
        var dataname = child.attr("data-name");
        if (dataname === "active") {
          if (child.find('input').is(':checked')) {
            form.find('[name="'+dataname+'"]').attr("checked", "yes");
          }
        } else {
          form.find('[name="'+dataname+'"]').val(child.text())
        }
      });
    });

    //delete account
    $this.find('.deleteAccountButton').on('click', function(ev) {
      var target = $(ev.currentTarget);
      var tr = target.parents("tr");
      var user = tr.children('[data-name="user"]').text();
      var host = tr.children('[data-name="host"]').text();
      if (user) {
        if (host) {
          $.get(settings.actionUrl, { "do": "delete", user: user, host: host }, function(data) {
            var target = $(ev.currentTarget);
            feedback(target.parents(".feedback"), data);
            if (data.success) {
              reloadAccounts($this);
            }
          });
        }
      }
    });
  }

  function render($this, settings) {
    $this.html(Mustache.render(addFormTemplate, settings));
    $this.append(Mustache.render(listFormTemplate, settings));
    var table = $('<div/>', {
      class: 'resultTable',
      html: Mustache.render(tableTemplate, {})
    });
    $this.append(table);
    addHandlers($this, settings);
  }

  function reloadAccounts($this) {
    $this.find('.listAccountsButton').trigger('click');
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('fetchmail-account-manager');

        if (!data) {
          var settings = $.extend({
            actionUrl: 'action/managefetchmailaccounts.json'
          }, options);
          $(this).data('fetchmail-account-manager', {
            target: $this,
            settings: settings
          });

          render($this, settings);
        }
      });
    }

  };

  $.fn.fetchmailAccountManager = function(method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jQuery.jquery.fetchmailaccount-manager.js');
    }
  };
})(jQuery);