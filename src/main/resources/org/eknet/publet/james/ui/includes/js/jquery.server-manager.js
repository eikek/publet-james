/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 15.12.12 01:05
 */
(function ($) {

  var boxTemplate =
      '<ul class="serverBoxes">' +
          '{{#servers}}' +
          '<li>' +
          '  <div class="serverType">{{serverType}}</div> ' +
          '  <div class="left">' +
          '    <span class="label label-{{startedLabel}}">' +
          '      {{#secure}}<i class="icon-lock icon-white"></i>{{/secure}}' +
          '      {{serverState}}' +
          '    </span> ' +
          '    <span>{{boundAddress}}</span>' +
          '    <span class="badge badge-info">{{currentConnections}}</span> active' +
          '  </div> ' +
          '  <div class="right">' +
          '    <a class="btn btn-primary btn-small" data-index="{{index}}" data-action="{{action}}">' +
          '      <i class="icon-{{action}} icon-white"></i>' +
          '    </a>' +
          '    <a class="btn btn-small widgetRefresh"><i class="icon-refresh"></i></a>' +
          '  </div>' +
          '</li>' +
          '{{/servers}}' +
          '</ul>';

  function renderError($this, msg) {
    $this.find('div[class="left"] span').first().addClass("alert alert-error").html(msg);
  }
  function reload($this, settings) {
    $this.mask();
    $.get(settings.actionUrl, { "do": "get", serverType: settings.serverType }, function(data) {
      var view = {
        servers: data
      };
      $this.unmask().html(Mustache.render(boxTemplate, view));
      if (data.success === false) {
        renderError($this, data.message);
      } else {
        addActionHandler($this, settings);
      }
      addRefreshHandler($this, settings);
    });
  }

  function addRefreshHandler($this, settings) {
    $this.find('.widgetRefresh').on('click', function(event) {
      reload($this, settings);
    });
  }

  function addActionHandler($this, settings) {
    $this.find('a[data-action]').on('click', function(event) {
      $this.mask();
      var options = {
        "do": $(event.currentTarget).attr("data-action"),
        "index": $(event.currentTarget).attr("data-index"),
        "serverType": settings.serverType
      };
      $.post(settings.actionUrl, options, function(data) {
        if (data.success) {
          reload($this, settings);
        } else {
          renderError($this, data.message);
        }
      });
    });
  }

  var methods = {
    init: function (options) {
      return this.each(function () {
        var $this = $(this);
        var data = $this.data('server-manager');

        if (!data) {
          var settings = $.extend({
            serverType: "smtp",
            actionUrl: "action/manageserver.json"
          }, options);

          $(this).data('server-manager', {
            target: $this,
            settings: settings
          });
          reload($this, settings);
        }
      });
    },

    reload: function () {
      var target = this.data('server-manager').target;
      var settings = this.data('server-manager').settings;
      reload(target, settings);
      return this;
    }
  };

  $.fn.serverManager = function(method) {
    if (methods[method]) {
      return methods[ method ].apply(this, Array.prototype.slice.call(arguments, 1));
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments);
    } else {
      $.error('Method ' + method + ' does not exist on jQuery.jquery.server-manager.js');
    }
  };
})(jQuery);