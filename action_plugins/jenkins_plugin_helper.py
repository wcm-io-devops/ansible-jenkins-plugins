#!/usr/bin/python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

__metaclass__ = type

import yaml
import os
import re

from ansible.module_utils._text import to_native
from ansible.plugins.action import ActionBase
from ansible.errors import AnsibleOptionsError

try:
    from __main__ import display
except ImportError:
    from ansible.utils.display import Display
    display = Display()


class ActionModule(ActionBase):
    TRANSFERS_FILES = False

    def __init__(self, task, connection, play_context, loader, templar, shared_loader_obj):
        super(ActionModule, self).__init__(task, connection, play_context, loader, templar, shared_loader_obj)
        self._task_vars = None

    def run(self, tmp=None, task_vars=None):
        if task_vars is None:
            task_vars = dict()

        result = super(ActionModule, self).run(tmp, task_vars)

        self._task_vars = task_vars

        try:
            plugins_installed = self._get_arg_or_var('plugins_installed', {})
            plugins_present = self._get_arg_or_var('plugins_present', [])
            plugins_absent = self._get_arg_or_var('plugins_absent', [])
        except AnsibleOptionsError as err:
            return self._fail_result(result, err.message)

        normalized_present_plugins =  self._normalize_plugins(plugins_present)
        normalized_absent_plugins =  self._normalize_plugins(plugins_absent)

        present_plugins = self._calculate_present_plugins(normalized_present_plugins, plugins_installed)
        uninstall_plugins = self._calculate_absent_plugins(normalized_absent_plugins, plugins_installed)

        result["ansible_facts"] = {
            "jenkins_plugins_install_plugins": present_plugins,
            "jenkins_plugins_uninstall_plugins": uninstall_plugins
        }

        return result

    def _calculate_absent_plugins(self, managed_plugins, existing_plugins):
        tmp = {}

        for key in managed_plugins:
            display.vv("Check if plugin [%s] has to be uninstalled" % (key))
            existing_plugin = existing_plugins.get(key, None)
            if existing_plugin is not None:
                display.vv("Mark plugin [%s] as absent " % (key))
                tmp[key] = {
                    "state": "absent",
                }

        return tmp

    def _calculate_present_plugins(self, managed_plugins, existing_plugins):
        tmp = {}

        for key in managed_plugins:
            managed_plugin = managed_plugins.get(key)

            try:
                managed_plugin_absent = managed_plugin.get("absent", False)
                managed_plugin_version = managed_plugin.get("version", None)
                managed_plugin_latest = managed_plugin.get("latest", False)
            except AnsibleOptionsError as err:
                managed_plugin_version = None
                managed_plugin_latest = False
                managed_plugin_absent = False

            if managed_plugin_absent is True:
                continue

            existing_plugin = existing_plugins.get(key, None)

            plugin_facts = False

            if existing_plugin is None:
                display.v("needs installation: [%s] %s" % (key, managed_plugin))
                if managed_plugin_latest is True:
                    # install plugin in lastest version
                    plugin_facts = {
                        "state": "latest"
                    }
                elif managed_plugin_version is not None:
                    # install plugin in specific version
                    plugin_facts = {
                        "state": "present",
                        "version": managed_plugin_version
                    }
                else:
                    # no specific version, just make it present
                    plugin_facts = {
                        "state": "present",
                    }
            else:
                display.v("already installed, checking versions: [%s] %s" % (key, managed_plugin))
                existing_plugin_version = existing_plugin.get("version")
                existing_plugin_hasUpdate = existing_plugin.get("hasUpdate")

                # update plugin when possible
                if managed_plugin_latest is True and existing_plugin_hasUpdate is True:
                    # update plugin to latest when update is available
                    plugin_facts = {
                        "state": "latest"
                    }
                # install plugin with specific version
                elif managed_plugin_version is not None and managed_plugin_version != existing_plugin_version:
                    # change plugin to specific version if defined
                    plugin_facts = {
                        "state": "present",
                        "version": managed_plugin_version
                    }

            if plugin_facts is not False:
                display.vv("Change required for plugin: [%s], plugin facts: %s, new: %s, old: %s" % (key, plugin_facts, managed_plugin, existing_plugin))
                tmp[key] = plugin_facts

        return tmp

    def _normalize_plugins(self, plugins):
        tmp = {}
        display.vv("_normalize_plugins: %s" % (plugins))

        for plugin in plugins:
            display.v("_normalize_plugin: %s" % (plugin))
            normalized_managed_plugin = {
                "version": plugin.get("version", None),
                "enabled": plugin.get("enabled", True),
                "pinned": plugin.get("pinned", False),
                "absent": plugin.get("absent", False),
                "latest": plugin.get("latest", False),

            }
            tmp[plugin.get('name')] = normalized_managed_plugin
        return tmp

    def _normalize_existing_plugins(self, existing_plugins):
        tmp = {}

        for existing_plugin in existing_plugins.get("plugins", []):
            # Always display resolved role and mapping
            existing_plugin_name = existing_plugin.get("shortName")
            normalized_existing_plugin = {
                "version": existing_plugin.get("version"),
                "enabled": existing_plugin.get("active"),
                "pinned": existing_plugin.get("pinned"),
                "hasUpdate": existing_plugin.get("hasUpdate"),
                "absent": False
            }
            tmp[existing_plugin_name] = normalized_existing_plugin
        return tmp

    @staticmethod
    def _fail_result(result, message):
        result['failed'] = True
        result['msg'] = message
        return result

    def _get_arg_or_var(self, name, default=None, is_required=True):
        ret = self._task.args.get(name, self._task_vars.get(name, default))
        display.v("_get_arg_or_var %s, default: %s, required: %s, ret: %s" % (name, default, is_required, ret))
        if is_required and not ret and ret != default:
            raise AnsibleOptionsError("parameter %s is required" % name)
        else:
            return ret
