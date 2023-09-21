[![CI](https://github.com/wcm-io-devops/ansible-jenkins-plugins/workflows/CI/badge.svg?branch=master&event=push)](https://github.com/wcm-io-devops/ansible-jenkins-plugins/actions?query=workflow%3ACI)

# wcm_io_devops.jenkins_plugins

This role manages the installation, update and uninstallation of Jenkins
Plugins in a faster way than simply using the `jenkins_plugin`.

It uses the action plugin `jenkins_plugin_helper` supplied with this
role to calculate the necessary install/uninstall actions to avoid
unnecessary timeconsuming executions of the `jenkins_plugin` module.

## Requirements

This role requires Ansible 2.7 or higher and a running Jenkins on the
target instance.

## Role Variables

Available variables are listed below, along with their default values:

    jenkins_plugins_present: []
    # format:
    #  - name: [plugin-short-name]
    #    version: "[plugin-version]"

List of plugins that should be present/installed.

:bulb: The use of quotation marks is strictly recommended! Otherwise
versions like `2.4.0` would be converted into `2.4` which might result
in failures.

    jenkins_plugins_absent: []
    # format:
    #  - name: [plugin-short-name]

List of plugins that should be absent/uninstalled.

    jenkins_plugins_admin_username: admin

Jenkins admin password.

    jenkins_plugins_admin_password: admin

Jenkins admin username.

    jenkins_plugins_jenkins_home: /var/lib/jenkins

Path to the jenkins directory.

    jenkins_plugins_jenkins_hostname: localhost

Hostname of the jenkins instance.

    jenkins_plugins_jenkins_port: 8080

HTTP port of the jenkins instance.

    jenkins_plugins_jenkins_url_prefix: ""

Url prefix of the jenkins instance, e.g. when running in tomcat.

    jenkins_plugins_jenkins_update_dir: "{{ jenkins_plugins_jenkins_home }}/updates"

Path to the jenkins update directory.

    jenkins_plugins_jenkins_base_url: "http://{{ jenkins_plugins_jenkins_hostname }}:{{ jenkins_plugins_jenkins_port }}{{ jenkins_plugins_jenkins_url_prefix }}"

The base url of the jenkins instance.

    jenkins_plugins_updates_expiration: 86400

Maximum seconds since the last jenkins plugin update check.

    jenkins_plugins_updates_timeout: 60

Timeout for jenkins update operation.

    jenkins_plugins_updates_base_url: https://updates.jenkins.io

Base URL to jenkins update site.

    jenkins_plugins_updates_update_site_url: "{{ jenkins_plugins_updates_base_url }}/update-center.json"

Complete URL to update-center.json.

    jenkins_plugins_download_base_url: "{{ jenkins_plugins_updates_base_url }}"

Download base URL for the jenkins plugins (you can point this to a local mirror to speed up plugin installation).

    jenkins_plugins_failedplugins_check: true

Controls if the role will fail when plugins are in failed state after installation.

    jenkins_plugins_debug: false

When set to enable the role will log some debug information.

    jenkins_plugins_proxy_username: ""

The username for the update center proxy.

    jenkins_plugins_proxy_password: ""

The password for the update center proxy.

    #jenkins_plugins_proxy_host: ""

The update center proxy host, only evaluated when jenkins_plugins_proxy_port is set.

    #jenkins_plugins_proxy_port: ""

The update center proxy port, only evaluated when jenkins_plugins_proxy_host is set.

    jenkins_plugins_proxy_no_proxy_host: ""

The update center proxy no proxy hosts list.

    jenkins_plugins_complete_initial_setup: false

Controls if the initial setup of the jenkins instance should be completed.
This may be necessary in proxy environments where the jenkins instance still thinks it is in offline mode.

    jenkins_plugins_owner: "jenkins"

User owning the files in the plugins folder

    jenkins_plugins_group: "jenkins"

Group owning the files in the plugins folder

## Dependencies

This role depends on the
[wcm_io_devops.jenkins_facts](https://github.com/wcm-io-devops/ansible-jenkins-facts)
role to retrieve the list of installed plugins from the Jenkins
instance.

It also depends on the
[wcm_io_devops.jenkins_service](https://github.com/wcm-io-devops/ansible-jenkins-service)
role to restart the Jenkins instance when plugins were installed/updated
or uninstalled.

## Example Playbook

Installs the greenballs plugin in version 1.15 on the target instance.

	- hosts: jenkins
	  vars:
	    jenkins_plugins_present:
	        - name: greenballs
	          version: "1.15"
	  roles:
	    - role: wcm_io_devops.jenkins_plugins

## License

Apache 2.0
