# Apache James Extension for Publet

This is an extension for [publet](https://eknet.org/main/projects/publet/index.html) that
adds the [Apache James](http://james.apache.org) server to the play.

With this extension publet advances to an email platform.

The complete documentation is provided in the source tree at
`src/main/resources/org/eknet/publet/james/doc/` and visible if this extension is
added to a publet instance.

## Configuration

There are 3 server threads started by default providing the following services

1. SMTP, bound to `0.0.0.0`, port `9025` (StartTLS)
2. IMAPv4, bound to `0.0.0.0`, port `9993` (SSL)
3. POP3, bound to `0.0.0.0`, port `9995` (SSL)
4. Fetchmail. See the documentation below.

By default, all services are configured to run over SSL (imap and pop3) or
use StartTLS (smtp). The certificate is looked up from the keystore `etc/keystore.ks`
and if that does not exist, a default self-signed certificate is created.

The ports are the standard ports with an offset of `9000`, because on linux systems
users other than root are not allowed to bind ports below `1024`. The tool `iptables`
or something similiar can be used to forward traffic from the standard ports to those:

    iptables -t nat -A PREROUTING -i eth0 -p tcp --dport 25 -j REDIRECT --to-port 9025

I found it easier while testing, because when removing this rule I can safely test on
the local port without outside access.

#### Users

The james extension uses the user service provided by publet. Every user that
is added to the group `mailgroup` is allowed to connect to the mail servers
using his password. The james server(s) do only see users belonging to this
group, all others are not visible to James. Thus, to be able to send/receive
mails or browse the mailboxes, add your users to the `mailgroup`.


#### Default Configuration Files

This extension provides default configuration for james that aims to be as
sensible as possible. These are the templates provided by James with only
slight modifications. The configuration is provided by the following files:

* `domainlist.conf`
* `imapserver.conf`
* `smtpserver.conf`
* `pop3server.conf`
* `mailetcontainer.conf`

If any such file is placed in the `etc/` directory, the default file is
discarded.

James resources specified in the configuration files are mapped to publet's
file system structure. Resources `file://conf/x.y` are mapped to `etc/x.y`
and `file://var/x.y` are mapped to `$PUBLET_CONFIG/james/var`.

If you just want to change certain default properties (like for example the
keystore password) then you can add those to publet's configuration file
`publet.properties` instead of creating complete new xml files. The keys
must be prefixed with `publet.james.conf.`. Each james default config value
is replaced with an existing value in `publet.properties`.

So to change the keystore password for the provided servers, add it
as follows:

    publet.james.conf.smtpserver.tls.secret=mysuperword123
    publet.james.conf.imapserver.tls.secret=mysuperword123
    publet.james.conf.pop3server.tls.secret=mysuperword123

This way any default value can be replaced, though it's not possible to
remove or add values.

### Fetchmail

A fetchmail background job can be used to fetch external mail and deliver it to
local accounts.

#### James Fetchmail Scheduler

James provides a `FetchScheduler` that can be configured by xml files. You need
to create a configuration file `etc/fetchmail.conf`. Please see
[James Documentation](http://james.apache.org/server/3/config-fetchmail.html) for
how to configure it.

James' fetchmail scheduler can be configured in various ways and is very powerful. Many
options should be carefully set by an admin user.

#### Integrated Scheduler

This extension provides another fetchmail scheduler that can be configured via the
web interface. It aims to be more easy to use, but lacks some of the features compared
to James' "native" scheduler.

The configuration is divided into two parts: First, regular user can manage their accounts
via the web. All those account are processed by one job that is executed periodically. The
admin user can edit the interval of this job, as well as starting or stopping it. The user
can configure for each of his accounts, at which multiple of the run it should be processed.
For example, on every run or on every second run etc.

Secondly, instead of configuring the maximum number of threads to use, you can configure
how many accounts should be processed sequentially by one thread. The fetchmail job is
collecting this number of accounts and schedules a fetch-job for each pile. You can configure
this number in publets' configuration file:

    james.fetchmail.jobsize=10

The default value is 10. So if there are 100 accounts, there can be up to ten threads
(depending on your thread-pool configuration) running concurrently fetching mails. This
feature relys on the [Quartz Scheduler](http://quartz-scheduler.org/) library, meaning
the [publet-quartz extension](../publetquartzmodule/) must be available. So you can still
configure the thread pool using `quartz.properties` configuration file. Please see the
[publet-quartz extension](../publetquartzmodule/) for how to do that.

### Sieve

[Apache James](http://james.apache.org) supports filtering incoming mails using
[Sieve](http://en.wikipedia.org/wiki/Sieve_(mail_filtering_language) scripts. The sieve
scripts are hold inside a git repository `publet-james-sieve` that is mounted to
`publet/james/sieve/`. You can restrict access to the repository by adding a repository
model entry to the `repositories.xml` file as explained in the
[Security Section](https://eknet.org/publet/doc/security.html#Repositories_file_explained) of publet's documentation.

There is a JQuery widget that allows editing the scripts online
if the corresponding permissions are set. Besides this, you can clone the `publet-james-sieve`
repository and push changes back to the server. They are immediately available to james
after a `git push`.

The sieve scripts are named `<loginname>.sieve` and are put at the root of the `publet-james-sieve`
content.

### Custom Mailets

It is very easy to hook into the mail processing chain with Apache James. This is done
by implementing a `Mailet` and configuring it in `mailetcontainer.conf` configuration file.

This extension provides a mailet (it is active for all mails by default) that will forward any
mail to publet's event bus. Thus, to hook into the mail processing chain, just add a
singleton to your guice module and `@Subscribe` to the `IncomeMailEvent`. The `IncomeMailEvent`
object holds a reference to the mail and the `MailetConfig` such that subscribers can
alter the mail like a normal `Mailet`. This forwarding mailet is added to the standard
transport processor and gets notified for every mail that is locally or remotely delivered
by James.

If you provide a custom `mailetcontainer.conf` file in the `etc/` directory, you might
want to enable this feature via:

    <!-- publet mailet that posts mails on the global event bus -->
    <mailet match="All" class="org.eknet.publet.james.mailets.EventBusMailet"/>

Mailets declared in the `mailetcontainer.conf` file are instantiated using Guice. That
means the dependencies of each mailet (and matcher) are automatically injected.

For example, you can now add scala classes to the `startup` folder and subscribe to
`IncomeMailEvent`s to react on any incoming mails without touching any code. All
that is needed is an appropriate scala class in `/main/.allIncludes/startup/` folder.

#### SimpleMailingListHeaders

Using the recipient rewrite table feature of James, you can easily forward mails to one
address to many recipients. This can be used to create simple small mailing lists,
since mails are delivered to remote and local recipients. The mailet `SimpleMailingListHeaders`
now can add the appropriate headers to such mails.

If you have declared such a mailing list mapping in the virtual address table, just add
the address to your `settings.properties`:

    james.mailing-lists=mylist@mydomain.com

Any mail to this address is now enhanced with some more headers identifying this mail
as a mailing list mail.

## Manage

The mail servers can be managed via a provided web interface. There are three templates

* `/publet/james/manage.html`
* `/publet/james/mailsettings.html` and
* `/publet/james/report.html`

The first one is intended for the admin users as it exposes all available settings. The
second one includes a widget for managing aliases for the current user his fetchmail
accounts and other user specific things. The third one shows a report of james with some
numbers (like number of failed vs successful logins etc).

If you look at the source of the templates (just use the extension `page`), you'll see that
all widgets are provided by JQuery plugins. That makes it very easy to create your own
page and rearrange or drop widgets as you desire.

If you put the widgets in other pages, you might have to adjust the url to the json
servlets. You also need to include the asset group `publet.james` in your page.