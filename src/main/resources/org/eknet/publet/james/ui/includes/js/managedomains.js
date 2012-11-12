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
    function loadDomains() {
        var parent = $('.domainList').mask();
        var removeButton = $('<a/>', {
           html: '<i class="icon-trash"></i>',
           class: 'domainRemoveButton btn btn-mini'
        });
        $.get("/publet/james/action/getdomains.json", function(domains) {
            $.get("/publet/james/action/getdefaultdomain.json", function(defaultDomain) {
                parent.empty().unmask();
                for (var i=0; i<domains.length; i++) {
                    var btn = removeButton.clone(true);
                    btn.attr("data-domain", domains[i]);
                    var li = $('<li/>', {
                        html: btn
                    });
                    li.append("  &nbsp;"+ domains[i]);
                    if (domains[i] == defaultDomain) {
                        li.addClass("defaultDomain");
                        btn.attr("disabled", "disabled");
                    }
                    parent.append(li);
                }
                addRemoveHandlers();
            })
        })
    }

    function feedback(data) {
        var msg = $('<p/>', {
            html: data.message,
            class: data.success ? "alert alert-success" : "alert alert-error"
        });
        $('.domainFeedback').html(msg).animate({delay:1}, 3500, function() { $('.domainFeedback').html(""); })
    }

    $('#domainAddForm').ajaxForm({
        beforeSubmit: function(arr, form, options) {
            form.mask();
        },
        success: function(data, status, xhr, form) {
            form.unmask().clearForm();
            loadDomains();
            feedback(data);
        }
    });

    function addRemoveHandlers() {
        $('.domainRemoveButton').click(function(event) {
            var disabled = $(event.target).attr("disabled")  || $(event.target.parentElement).attr("disabled");
            if (!disabled) {
                var domain = $(event.target).attr("data-domain")  || $(event.target.parentElement).attr("data-domain");
                if (domain) {
                    $(event.target).mask();
                    $.get("/publet/james/action/removedomain.json", {domain: domain}, function(result) {
                        $(event.target).unmask();
                        loadDomains();
                        feedback(result);
                    });
                }
            }
        });
    }
    loadDomains();
});
