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

def instance = Jenkins.getInstance()
def proxy = ProxyConfiguration.load()

String newUserName = "$proxy_username"
String newPassword = "$proxy_password"
String newHost = "$proxy_host"
int newPort = $proxy_port
String newNoProxyHost = "$proxy_no_proxy_host"
Boolean completeInitialSetup = $complete_initial_setup
Boolean installStageChanged = false

// reset to null when empty
if (newUserName == "") {
  newUserName = null
}

if (newNoProxyHost == "") {
  newNoProxyHost = null
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
    newNoProxyHost
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

Boolean hashChanged = (newHash != actualHash)
if (hashChanged) {
  newProxy = new ProxyConfiguration(newHost, newPort, newUserName, newPassword, newNoProxyHost)
  newProxy.save()
  instance.save()
}

// complete setup when enabled, since jenkins may think he is still offline
if (completeInitialSetup && !instance.installState.isSetupComplete()) {
  InstallState.INITIAL_SETUP_COMPLETED.initializeState()
  installStageChanged = true
}

return (hashChanged || installStageChanged)