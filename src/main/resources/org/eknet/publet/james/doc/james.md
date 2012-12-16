## James Extension

This extension adds [Apache James](http://james.apache.org) to publet,
turning it into a email server. A simple web interface is provided to
manage the server.

### Configuration

There are 3 server threads started by default providing the following services

1. SMTP, bound to `0.0.0.0`, port `9025` (StartTLS)
2. IMAPv4, bound to `0.0.0.0`, port `9993` (SSL)
3. POP3, bound to `0.0.0.0`, port `9995` (SSL)

By default, all services are configured to run over SSL (imap and pop3) or
use StartTLS (smtp). The certificate is looked up from the keystore `etc/keystore.ks`
and if that does not exist, a default self-signed certificate is created. The
ports are the standard ports with an offset of `9000`, because on linux systems
users other than root are not allowed to bind ports below `1024`. The tool `iptables`
or something similiar can be used to forward traffic from the standard ports to those.

#### Default Configuration Files

This extension provides default configuration for james that aims to be as
sensible as possible. These are the templates provided by James with only
slight modifications. The configuration is provided by the following files:

* [domainlist.conf](domainlist.html)
* [imapserver.conf](imapserver.html)
* [smtpserver.conf](smtpserver.html)
* [pop3server.conf](pop3server.html)
* [mailetcontainer.conf](mailetcontainer.html)

If any such file is placed in the `etc/` directory, the default file is
discarded.

James resources specified in the configuration files are mapped to publet's
file system structure. Resources `file://conf/x.y` are mapped to `etc/x.y`
and `file://var/x.y` are mapped to `$PUBLET_CONFIG/james/var`.


### Fetchmail

A fetchmail background job can be used to fetch external mail and deliver it to
local accounts.

It is possible to define only one job that runs repeatedly in a configured interval. The
job goes through all configured accounts, fetches the mails and delivers them locally.
While there is one job, users can decide on which multiple of a run their accounts
should be processed. Thus the interval is the minimum interval for all users.


### Custom Mailets

It is very easy to hook into the mail processing chain with Apache James. This is done
by implementing a `Mailet` and configure it in `mailetcontainer.conf` configuration file.

The default mailetcontainer contains a mailet from this extension that will forward any
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

#### SimpleMailingListHeaders

Using the recipient rewrite table feature of James, you can easily forward mails to one
address to many recipients. This can be used to create simple small mailing lists,
since mails are delivered to remote and local recipients. The mailet `SimpleMailingListHeaders`
now can add the appropriate headers to such mails.

If you have declared such a mailing list mapping in the virtual address table, just add
the address to your `settings.properties`:

    publet.james.mailing-lists=mylist@mydomain.com

Any mail to this address is now enhanced with some more headers identifying this mail
as a mailing list mail.
