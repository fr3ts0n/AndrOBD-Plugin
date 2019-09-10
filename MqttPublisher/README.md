# MQTT publisher plugin

This extension plugin allows to publish AndrOBD data items (PIDs) to MQTT automation server.

## Functionality

- The AndrOBD MQTT publisher published OBD vehicle data gathered from AndrOBD to a configurable MQTT server.
- The extension connects to a single MQTT message broker as a publishing client using existing network connection.
- All AndrOBD data items which are selected for display/update can be selected to be published to MQTT.
- Publications are sent as a bulk of separate MQTT messages.
- Publications are triggered automatically in a cyclic loop.
  - The time between publications is configurable.
- The publication may also be triggered manually fromout the host application.

## Configuration

Following generic parameters for publication shall be configurable:

- MQTT parameters
  - Host Name / IP address
  - Port number
  - User Name
  - Password
  - MQTT message prefix

- Update parameters
  - Update cycle time [s]
  - OBD data items
    - Selection of subset of AndrOBD data items for display.
