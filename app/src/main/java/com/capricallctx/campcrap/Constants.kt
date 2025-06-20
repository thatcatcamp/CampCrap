/*
 * Copyright 2025 CAT Camp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.capricallctx.campcrap

object Constants {
    const val CURRENT_YEAR = "2025"
    
    // Spreadsheet names
    const val PEOPLE_SPREADSHEET_PREFIX = "People_Inventory"
    const val STUFF_SPREADSHEET_PREFIX = "Stuff_Inventory"
    
    // Default headers for spreadsheets
    val PEOPLE_HEADERS = listOf("Name", "Email", "Phone", "Arrival Date", "Departure Date", "Camp Role", "Notes")
    val STUFF_HEADERS = listOf("Item Name", "Category", "Quantity", "Location", "Owner", "Notes", "Date Added")
}