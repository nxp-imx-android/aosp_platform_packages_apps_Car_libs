# Car UI Lib as a shared library

## Background
OEM customization of applications using the car-ui-lib requires the creation of an RRO that targets the specific application the OEM wants to modify, even if those values are deemed to be common. This means the OEM is required to have a RRO per application, but the real downside is that the OEM must have said RRO preinstalled and thus it’s required to know ALL applications using car-ui-lib at the system build time.

The newer option is for the OEM to implement a car-ui-plugin that contains a component factory. This plugin can be found by the static implementation of the car-ui-lib which will delegate its rendering to the plugin if it’s found. The down side of this is that this solution is very much overkill if the customization desired is just that of changing a resource.

## Solution
The car-ui-lib-sharedlibrary target is wrapper around static implimenation of car-ui-lib providing a target for which an OEM RRO can modify the resources contained within. This directory contains the build target and an example RRO both of which need to be pre-installed for them to be available at runtime.

The plugin-proxy is also required so apps don't need to be aware of this implimenation, which is contained in a different directory.
