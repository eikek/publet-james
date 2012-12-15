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

The default configuration can be overriden by dropping the necessary james
configuration files in the `etc/` directory. The only those files are used to configure
james and all default values provided by this extension are ignored.


### Fetchmail

A fetchmail background job can be used to fetch external mail and deliver it to
local accounts.

It is possible to define only one job that runs repeatedly in a configured interval. The
job goes through all configured accounts, fetches the mails and delivers them locally.
While there is one job, users can decide on which multiple of a run their accounts
should be processed. Thus the interval is the minimum interval for all users.
