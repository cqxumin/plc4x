/*
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
*/

#include <plc4c/plc4c.h>
#include <plc4c/spi/types_private.h>
#include <stdlib.h>
#include <string.h>

enum plc4c_driver_modbus_read_states {
  PLC4C_DRIVER_MODBUS_READ_INIT,
  PLC4C_DRIVER_MODBUS_READ_FINISHED
};

plc4c_return_code plc4c_driver_modbus_read_machine_function(
    plc4c_system_task* task) {

}

plc4c_return_code plc4c_driver_modbus_read_function(
    plc4c_read_request_execution* read_request_execution,
    plc4c_system_task** task) {
  plc4c_system_task* new_task = malloc(sizeof(plc4c_system_task));
  new_task->state_id = PLC4C_DRIVER_MODBUS_READ_INIT;
  new_task->state_machine_function = &plc4c_driver_modbus_read_machine_function;
  new_task->completed = false;
  new_task->context = read_request_execution;
  new_task->connection = read_request_execution->system_task->connection;
  *task = new_task;
  return OK;
}

void plc4c_driver_modbus_free_read_response_item(
    plc4c_list_element* read_item_element) {
  plc4c_response_value_item* value_item =
      (plc4c_response_value_item*)read_item_element->value;
  plc4c_data_destroy(value_item->value);
  value_item->value = NULL;
}

void plc4c_driver_modbus_free_read_response(plc4c_read_response* response) {
  // the request will be cleaned up elsewhere
  plc4c_utils_list_delete_elements(response->items,
                                   &plc4c_driver_modbus_free_read_response_item);
}