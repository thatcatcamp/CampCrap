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