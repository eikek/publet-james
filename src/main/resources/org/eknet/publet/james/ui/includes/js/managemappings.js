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
$(function() {
  function loadMappings() {
    var parent = $('.mappingList').mask();
    var removeButton = $('<a/>', {
      html: '<i class="icon-trash"></i>',
      class: 'mappingRemoveButton btn btn-mini'
    });
    $.get("/publet/james/action/getmappings.json", function(mappings) {
        parent.empty().unmask();
        for (var key in mappings) {
            var el = $('<li/>', {
                html: key + ": "+ mappings[key]
            });
            parent.append(el);
        }
        parent.append(mappings);
    });
  }

  function feedback(data) {
    var msg = $('<p/>', {
      html: data.message,
      class: data.success ? "alert alert-success" : "alert alert-error"
    });
    $('.mappingFeedback').html(msg).animate({delay:1}, 3500, function() { $('.mappingFeedback').html(""); })
  }

  $('#mappingAddForm').ajaxForm({
    beforeSubmit: function(arr, form, options) {
      form.mask();
    },
    success: function(data, status, xhr, form) {
      form.unmask().clearForm();
      loadMappings();
      feedback(data);
    }
  });
  loadMappings();
});
