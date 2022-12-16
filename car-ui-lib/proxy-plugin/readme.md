# Car UI Lib as a shared library

## Background
OEM customization of applications using the car-ui-lib requires the creation of an RRO that targets the specific application the OEM wants to modify, even if those values are deemed to be common. This means the OEM is required to have a RRO per application, but the real downside is that the OEM must have said RRO preinstalled and thus it’s required to know ALL applications using car-ui-lib at the system build time.

The newer option is for the OEM to implement a car-ui-plugin that contains a component factory. This plugin can be found by the static implementation of the car-ui-lib which will delegate its rendering to the plugin if it’s found. The down side of this is that this solution is very much overkill if the customization desired is just that of changing a resource.

## Solution
The car-ui-lib-proxyplugin is example plugin that redirects it's implimenation back to the static version of car-ui-lib via going through the car-ui-lib-sharedlibrary. This allows for the plugin to be backed by a RRO targetable set of resoureces.
The plugin-proxy is only an example and starting place to do OEM customization via a plugin.

###Note
For more information on plugins see
https://source.android.com/docs/devices/automotive/hmi/car_ui/plugins


