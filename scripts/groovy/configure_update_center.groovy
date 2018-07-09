/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jenkins.model.*
import jenkins.hudson.*
import jenkins.install.InstallState
import hudson.model.UpdateSite
import hudson.util.PersistedList

def instance = Jenkins.getInstance()

// update site configuration

UpdateCenter updateCenter = instance.getUpdateCenter();
UpdateSite updateSite = updateCenter.getById(UpdateCenter.ID_DEFAULT)
String newUrl = "$updates_url"
String currentUrl = updateSite.getUrl()
Boolean updateSiteChanged = !newUrl.equalsIgnoreCase( currentUrl )

if (updateSiteChanged) {
  PersistedList<UpdateSite> sites = updateCenter.getSites();
  for (UpdateSite s : sites) {
    if (s.getId().equals(UpdateCenter.ID_DEFAULT))
      sites.remove(s)
  }
  sites.add(new UpdateSite(UpdateCenter.ID_DEFAULT, newUrl));
}

// proxy configuration
def proxy = ProxyConfiguration.load()

String newUserName = "$proxy_username"
String newPassword = "$proxy_password"
String newHost = "$proxy_host"
int newPort = $proxy_port
String newNoProxyHosts = "$proxy_no_proxy_hosts"
Boolean hashChanged = false

// only configure proxy when host and port are defined
if (newHost != "" && newPort != -1) {
    // reset to null when empty
    if (newUserName == "") {
      newUserName = null
    }

    if (newNoProxyHosts == "") {
      newNoProxyHosts = null
    } else {
      // convert comma separated newNoProxyHosts list to string with new line
      newNoProxyHosts = newNoProxyHosts.split(',').join('\n')
    }

    String actualUserName = null
    String actualPassword = null
    String actualHost = null
    int actualPort
    String actualNoProxyHost = null

    if (proxy) {
      actualUserName = proxy.userName
      actualPassword = proxy.getPassword()
      actualHost = proxy.name
      actualPort = proxy.port
      actualNoProxyHost = proxy.noProxyHost
    }

    List newHashParts = [
      newUserName,
      newPassword,
      newHost,
      newPort,
      newNoProxyHosts
    ]

    List actualHashParts = [
      actualUserName,
      actualPassword,
      actualHost,
      actualPort,
      actualNoProxyHost
    ]

    String newHash = newHashParts.join("")
    String actualHash = actualHashParts.join("")

    hashChanged = (newHash != actualHash)
    if (hashChanged) {
      newProxy = new ProxyConfiguration(newHost, newPort, newUserName, newPassword, newNoProxyHosts)
      newProxy.save()
      instance.save()
    }
}


// complete setup when enabled, since jenkins may think he is still offline
Boolean completeInitialSetup = $complete_initial_setup
Boolean installStageChanged = false

if (completeInitialSetup && !instance.installState.isSetupComplete()) {
  InstallState.INITIAL_SETUP_COMPLETED.initializeState()
  installStageChanged = true
}

return (hashChanged || installStageChanged || updateSiteChanged)