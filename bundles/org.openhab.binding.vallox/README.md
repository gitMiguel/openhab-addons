# Vallox Binding

This binding connects to Vallox central venting units. 
* Serial and tcp connections with older SE models.

## Supported Things

This binding supports two different things.
* SE Serial connection
* SE TCP/IP connection

## Thing Configuration

Binding has following configuration parameters:

### SE Serial

| Parameter                     | Type    | Required | Default if omitted               | Description                             |
| ------------------------------| ------- | -------- | -------------------------------- |-----------------------------------------|
| `serialPort`               | String  |   yes    | `none`                         | Serial port to connect to               |
| `panelNumber`             | Integer |   yes     | `none`                         | Panel number to use (choose between 1-8)|


### SE TCP/IP

| Parameter                     | Type    | Required | Default if omitted               | Description                             |
| ------------------------------| ------- | -------- | -------------------------------- |-----------------------------------------|
| `tcpHost`                  | String  |   yes     | `none`                         | IP number to connect to                 |
| `tcpPort`                  | Integer |   yes     | `none`                         | Port to use when connecting             |
| `panelNumber`             | Integer |   yes     | `none`                         | Panel number to use (choose between 1-8) |

## Channels

| Channel                    | Type    | Read only | Description                             |
| ------------------------------| ------- | -------- |-----------------------------------------|
| `xxxx`                  | xxxx  |   xxxx     | xxxx                 |



## Examples

### vallox.things

#### TCP/IP

```
Thing vallox:se-tcp:main [ tcpHost="192.168.0.57", tcpPort=26, panelNumber=5 ]
```

#### Serial

```
Thing vallox:se-serial:main [ serialPort="/dev/ttyUSB0", panelNumber=5 ]
```

Under Windows use normal com port names e.g. "COM3":

```
Thing vallox:se-tcp:main [ serialPort="COM3", panelNumber=8 ]
```

### vallox.items

You can use the following example to copy/paste the available items to your own item file

```
Group Vallox
Group ValloxAdvanced

Number FanSpeed (Vallox) { channel="vallox:kwl90se:main:FanSpeed" } 
Number FanSpeedMax (Vallox) { channel="vallox:kwl90se:main:FanSpeedMax" }
Number FanSpeedMin (Vallox) { channel="vallox:kwl90se:main:FanSpeedMin" }
Number TempInside (Vallox) { channel="vallox:kwl90se:main:TempInside" }
Number TempOutside (Vallox) { channel="vallox:kwl90se:main:TempOutside" }
Number TempExhaust (Vallox) { channel="vallox:kwl90se:main:TempExhaust" }
Number TempIncomming (Vallox) { channel="vallox:kwl90se:main:TempIncomming" }
Number InEfficiency (Vallox) { channel="vallox:kwl90se:main:InEfficiency" }
Number OutEfficiency  (Vallox) { channel="vallox:kwl90se:main:OutEfficiency" }
Number AverageEfficiency (Vallox) { channel="vallox:kwl90se:main:AverageEfficiency" }
Switch PowerState  (Vallox) { channel="vallox:kwl90se:main:PowerState" }
Number DCFanInputAdjustment (ValloxAdvanced) { channel="vallox:kwl90se:main:DCFanInputAdjustment" }
Number DCFanOutputAdjustment (ValloxAdvanced) { channel="vallox:kwl90se:main:DCFanOutputAdjustment" }
Number HrcBypassThreshold (ValloxAdvanced) { channel="vallox:kwl90se:main:HrcBypassThreshold" }
Number InputFanStopThreshold (ValloxAdvanced) { channel="vallox:kwl90se:main:InputFanStopThreshold" }
Number HeatingSetPoint (ValloxAdvanced) { channel="vallox:kwl90se:main:HeatingSetPoint" }
Number PreHeatingSetPoint (ValloxAdvanced) { channel="vallox:kwl90se:main:PreHeatingSetPoint" }
Number CellDefrostingThreshold (ValloxAdvanced) { channel="vallox:kwl90se:main:CellDefrostingThreshold" }
Switch CO2AdjustState (ValloxAdvanced) { channel="vallox:kwl90se:main:CO2AdjustState" }
Switch HumidityAdjustState (ValloxAdvanced) { channel="vallox:kwl90se:main:HumidityAdjustState" }
Switch HeatingState (ValloxAdvanced) { channel="vallox:kwl90se:main:HeatingState" }
Switch FilterGuardIndicator (ValloxAdvanced) { channel="vallox:kwl90se:main:FilterGuardIndicator" }
Switch HeatingIndicator (ValloxAdvanced) { channel="vallox:kwl90se:main:HeatingIndicator" }
Switch FaultIndicator (ValloxAdvanced) { channel="vallox:kwl90se:main:FaultIndicator" }
Switch ServiceReminderIndicator (ValloxAdvanced) { channel="vallox:kwl90se:main:ServiceReminderIndicator" }
Number Humidity (ValloxAdvanced) { channel="vallox:kwl90se:main:Humidity" }
Number BasicHumidityLevel (ValloxAdvanced) { channel="vallox:kwl90se:main:BasicHumidityLevel" }
Number HumiditySensor1 (ValloxAdvanced) { channel="vallox:kwl90se:main:HumiditySensor1" }
Number HumiditySensor2 (ValloxAdvanced) { channel="vallox:kwl90se:main:HumiditySensor2" }
Number CO2High (ValloxAdvanced) { channel="vallox:kwl90se:main:CO2High" }
Number CO2Low (ValloxAdvanced) { channel="vallox:kwl90se:main:CO2Low" }
Number CO2SetPointHigh (ValloxAdvanced) { channel="vallox:kwl90se:main:CO2SetPointHigh" }
Number CO2SetPointLow (ValloxAdvanced) { channel="vallox:kwl90se:main:CO2SetPointLow" }
Number AdjustmentIntervalMinutes (ValloxAdvanced) { channel="vallox:kwl90se:main:AdjustmentIntervalMinutes" }
Switch AutomaticHumidityLevelSeekerState (ValloxAdvanced) { channel="vallox:kwl90se:main:AutomaticHumidityLevelSeekerState" }
Switch BoostSwitchMode (ValloxAdvanced) { channel="vallox:kwl90se:main:BoostSwitchMode" }
Switch RadiatorType (ValloxAdvanced) { channel="vallox:kwl90se:main:RadiatorType" }
Switch CascadeAdjust (ValloxAdvanced) { channel="vallox:kwl90se:main:CascadeAdjust" }
Switch MaxSpeedLimitMode (ValloxAdvanced) { channel="vallox:kwl90se:main:MaxSpeedLimitMode" }
Number ServiceReminder (ValloxAdvanced) { channel="vallox:kwl90se:main:ServiceReminder" }
Switch PostHeatingOn (ValloxAdvanced) { channel="vallox:kwl90se:main:PostHeatingOn" }
Switch DamperMotorPosition (ValloxAdvanced) { channel="vallox:kwl90se:main:DamperMotorPosition" }
Switch FaultSignalRelayClosed (ValloxAdvanced) { channel="vallox:kwl90se:main:FaultSignalRelayClosed" }
Switch SupplyFanOff (ValloxAdvanced) { channel="vallox:kwl90se:main:SupplyFanOff" }
Switch PreHeatingOn (ValloxAdvanced) { channel="vallox:kwl90se:main:PreHeatingOn" }
Switch ExhaustFanOff (ValloxAdvanced) { channel="vallox:kwl90se:main:ExhaustFanOff" }
Switch FirePlaceBoosterClosed (ValloxAdvanced) { channel="vallox:kwl90se:main:FirePlaceBoosterClosed" }
Number IncommingCurrent (ValloxAdvanced) { channel="vallox:kwl90se:main:IncommingCurrent" }
Number LastErrorNumber (ValloxAdvanced) { channel="vallox:kwl90se:main:LastErrorNumber" }
```

### vallox.sitemap

See an example below for a sitemap that uses the vallox items.

```

sitemap demo label="Demo Sitemap" {
    
    Text label="Vallox" icon="fan"{
        Setpoint item=FanSpeed minValue=1 maxValue=8 step=1
        Setpoint item=FanSpeedMin minValue=1 maxValue=8 step=1
        Setpoint item=FanSpeedMax minValue=1 maxValue=8 step=1
        Group item=Vallox
        Group item=ValloxAdvanced
    }
}

```

## Troubleshooting

### Thing status

Check thing status and `openhab.log` for errors.

### Verbose logging

Enable DEBUG or even TRACE logging in karaf console to see more precise error messages:

`log:set DEBUG org.openhab.binding.vallox`

See [openHAB2 logging docs](http://docs.openhab.org/administration/logging.html#defining-what-to-log) for more help.
