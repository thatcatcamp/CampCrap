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

import android.content.Context
import android.os.Environment
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExcelService(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    fun exportData(year: String): File? {
        return try {
            val dbHelper = DatabaseHelper(context)
            val workbook = XSSFWorkbook()
            
            // Create header style
            val headerStyle = workbook.createCellStyle().apply {
                fillForegroundColor = IndexedColors.LIGHT_BLUE.getIndex()
                fillPattern = FillPatternType.SOLID_FOREGROUND
                val font = workbook.createFont().apply {
                    bold = true
                    color = IndexedColors.WHITE.getIndex()
                }
                setFont(font)
            }
            
            // Export Campers
            exportCampers(workbook, dbHelper, year, headerStyle)
            
            // Export Locations
            exportLocations(workbook, dbHelper, year, headerStyle)
            
            // Export Items
            exportItems(workbook, dbHelper, year, headerStyle)
            
            // Save to Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "CampCrap_${year}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.xlsx"
            val file = File(downloadsDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun exportCampers(workbook: Workbook, dbHelper: DatabaseHelper, year: String, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("Campers_$year")
        var rowNum = 0
        
        // Create header row
        val headerRow = sheet.createRow(rowNum++)
        val camperHeaders = arrayOf(
            "ID", "Name", "Email", "Real Name", "Entry Date", "Exit Date", 
            "Camp Name", "Notes", "Year", "Skipping", "Years Attended", 
            "Has Ticket Current Year", "Paid Dues Current Year", "Photo Path"
        )
        
        camperHeaders.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        // Get all campers (including infrastructure)
        val campers = dbHelper.getAllCampersAndInfrastructureForYear(year)
        
        // Add data rows
        for (camper in campers) {
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(camper.id.toDouble())
            row.createCell(1).setCellValue(camper.name)
            row.createCell(2).setCellValue(camper.email)
            row.createCell(3).setCellValue(camper.realName)
            row.createCell(4).setCellValue(camper.entryDate)
            row.createCell(5).setCellValue(camper.exitDate)
            row.createCell(6).setCellValue(camper.campName)
            row.createCell(7).setCellValue(camper.notes)
            row.createCell(8).setCellValue(camper.year)
            row.createCell(9).setCellValue(if (camper.skipping) "TRUE" else "FALSE")
            row.createCell(10).setCellValue(camper.yearsAttended)
            row.createCell(11).setCellValue(if (camper.hasTicketCurrentYear) "TRUE" else "FALSE")
            row.createCell(12).setCellValue(if (camper.paidDuesCurrentYear) "TRUE" else "FALSE")
            row.createCell(13).setCellValue(camper.photoPath ?: "")
        }
        
        // Set column widths manually (autoSizeColumn not supported on Android)
        sheet.setColumnWidth(0, 2000)  // ID
        sheet.setColumnWidth(1, 6000)  // Name
        sheet.setColumnWidth(2, 8000)  // Email
        sheet.setColumnWidth(3, 6000)  // Real Name
        sheet.setColumnWidth(4, 4000)  // Entry Date
        sheet.setColumnWidth(5, 4000)  // Exit Date
        sheet.setColumnWidth(6, 6000)  // Camp Name
        sheet.setColumnWidth(7, 8000)  // Notes
        sheet.setColumnWidth(8, 2000)  // Year
        sheet.setColumnWidth(9, 3000)  // Skipping
        sheet.setColumnWidth(10, 6000) // Years Attended
        sheet.setColumnWidth(11, 4000) // Has Ticket
        sheet.setColumnWidth(12, 4000) // Paid Dues
        sheet.setColumnWidth(13, 8000) // Photo Path
    }
    
    private fun exportLocations(workbook: Workbook, dbHelper: DatabaseHelper, year: String, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("Locations_$year")
        var rowNum = 0
        
        // Create header row
        val headerRow = sheet.createRow(rowNum++)
        val locationHeaders = arrayOf("ID", "Name", "Description", "Year")
        
        locationHeaders.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        // Get locations
        val locations = dbHelper.getLocationsForYear(year)
        
        // Add data rows
        for (location in locations) {
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(location.id.toDouble())
            row.createCell(1).setCellValue(location.name)
            row.createCell(2).setCellValue(location.description)
            row.createCell(3).setCellValue(location.year)
        }
        
        // Set column widths manually (autoSizeColumn not supported on Android)
        sheet.setColumnWidth(0, 2000)  // ID
        sheet.setColumnWidth(1, 8000)  // Name
        sheet.setColumnWidth(2, 10000) // Description
        sheet.setColumnWidth(3, 2000)  // Year
    }
    
    private fun exportItems(workbook: Workbook, dbHelper: DatabaseHelper, year: String, headerStyle: CellStyle) {
        val sheet = workbook.createSheet("Items_$year")
        var rowNum = 0
        
        // Create header row
        val headerRow = sheet.createRow(rowNum++)
        val itemHeaders = arrayOf(
            "ID", "Name", "Description", "Camper ID", "Camper Name", 
            "Location ID", "Location Name", "Photo Path", "Year", 
            "Created Date", "Removal Status"
        )
        
        itemHeaders.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        // Get all items (including removed)
        val items = dbHelper.getAllItemsForYear(year)
        
        // Add data rows
        for (item in items) {
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(item.id.toDouble())
            row.createCell(1).setCellValue(item.name)
            row.createCell(2).setCellValue(item.description)
            row.createCell(3).setCellValue(item.camperId.toDouble())
            row.createCell(4).setCellValue(item.camperName)
            row.createCell(5).setCellValue(item.locationId.toDouble())
            row.createCell(6).setCellValue(item.locationName)
            row.createCell(7).setCellValue(item.photoPath ?: "")
            row.createCell(8).setCellValue(item.year)
            row.createCell(9).setCellValue(item.createdDate)
            row.createCell(10).setCellValue(item.removalStatus)
        }
        
        // Set column widths manually (autoSizeColumn not supported on Android)
        sheet.setColumnWidth(0, 2000)  // ID
        sheet.setColumnWidth(1, 8000)  // Name
        sheet.setColumnWidth(2, 10000) // Description
        sheet.setColumnWidth(3, 3000)  // Camper ID
        sheet.setColumnWidth(4, 6000)  // Camper Name
        sheet.setColumnWidth(5, 3000)  // Location ID
        sheet.setColumnWidth(6, 6000)  // Location Name
        sheet.setColumnWidth(7, 8000)  // Photo Path
        sheet.setColumnWidth(8, 2000)  // Year
        sheet.setColumnWidth(9, 4000)  // Created Date
        sheet.setColumnWidth(10, 4000) // Removal Status
    }
    
    fun importData(file: File, targetYear: String, skipExisting: Boolean = true): ImportResult {
        return try {
            val dbHelper = DatabaseHelper(context)
            val workbook = XSSFWorkbook(FileInputStream(file))
            val result = ImportResult()
            
            // Import in order: Locations, Campers, Items (due to foreign key dependencies)
            importLocations(workbook, dbHelper, targetYear, skipExisting, result)
            importCampers(workbook, dbHelper, targetYear, skipExisting, result)
            importItems(workbook, dbHelper, targetYear, skipExisting, result)
            
            workbook.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(error = e.message)
        }
    }
    
    private fun importLocations(workbook: Workbook, dbHelper: DatabaseHelper, targetYear: String, skipExisting: Boolean, result: ImportResult) {
        try {
            // Look for location sheets (could be from any year)
            val locationSheets = (0 until workbook.numberOfSheets)
                .map { workbook.getSheetAt(it) }
                .filter { it.sheetName.startsWith("Locations_") }
            
            for (sheet in locationSheets) {
                val headerRow = sheet.getRow(0) ?: continue
                
                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    
                    try {
                        val name = getCellStringValue(row.getCell(1))
                        val description = getCellStringValue(row.getCell(2))
                        
                        if (name.isNotEmpty()) {
                            // Check if location exists for target year
                            val existingLocations = dbHelper.getLocationsForYear(targetYear)
                            val exists = existingLocations.any { it.name == name }
                            
                            if (!exists || !skipExisting) {
                                dbHelper.addLocation(name, description, targetYear)
                                result.locationsImported++
                            } else {
                                result.locationsSkipped++
                            }
                        }
                    } catch (e: Exception) {
                        result.errors.add("Location row ${i + 1}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            result.errors.add("Location import error: ${e.message}")
        }
    }
    
    private fun importCampers(workbook: Workbook, dbHelper: DatabaseHelper, targetYear: String, skipExisting: Boolean, result: ImportResult) {
        try {
            // Look for camper sheets (could be from any year)
            val camperSheets = (0 until workbook.numberOfSheets)
                .map { workbook.getSheetAt(it) }
                .filter { it.sheetName.startsWith("Campers_") }
            
            for (sheet in camperSheets) {
                val headerRow = sheet.getRow(0) ?: continue
                
                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    
                    try {
                        val name = getCellStringValue(row.getCell(1))
                        val email = getCellStringValue(row.getCell(2))
                        val realName = getCellStringValue(row.getCell(3))
                        val entryDate = getCellStringValue(row.getCell(4))
                        val exitDate = getCellStringValue(row.getCell(5))
                        val campName = getCellStringValue(row.getCell(6))
                        val notes = getCellStringValue(row.getCell(7))
                        val skipping = getCellBooleanValue(row.getCell(9))
                        val yearsAttended = getCellStringValue(row.getCell(10))
                        val hasTicket = getCellBooleanValue(row.getCell(11))
                        val paidDues = getCellBooleanValue(row.getCell(12))
                        
                        if (name.isNotEmpty()) {
                            // Check if camper exists for target year
                            val existingCampers = dbHelper.getAllCampersAndInfrastructureForYear(targetYear)
                            val exists = existingCampers.any { it.name == name && it.email == email }
                            
                            if (!exists || !skipExisting) {
                                dbHelper.addPerson(
                                    name = name,
                                    email = email,
                                    realName = realName,
                                    entryDate = entryDate,
                                    exitDate = exitDate,
                                    campName = campName,
                                    notes = notes,
                                    year = targetYear,
                                    isInfrastructure = name == "Camp Infrastructure",
                                    yearsAttended = yearsAttended,
                                    hasTicketCurrentYear = hasTicket,
                                    paidDuesCurrentYear = paidDues
                                )
                                result.campersImported++
                            } else {
                                result.campersSkipped++
                            }
                        }
                    } catch (e: Exception) {
                        result.errors.add("Camper row ${i + 1}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            result.errors.add("Camper import error: ${e.message}")
        }
    }
    
    private fun importItems(workbook: Workbook, dbHelper: DatabaseHelper, targetYear: String, skipExisting: Boolean, result: ImportResult) {
        try {
            // Look for item sheets (could be from any year)
            val itemSheets = (0 until workbook.numberOfSheets)
                .map { workbook.getSheetAt(it) }
                .filter { it.sheetName.startsWith("Items_") }
            
            for (sheet in itemSheets) {
                val headerRow = sheet.getRow(0) ?: continue
                
                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue
                    
                    try {
                        val name = getCellStringValue(row.getCell(1))
                        val description = getCellStringValue(row.getCell(2))
                        val camperName = getCellStringValue(row.getCell(4))
                        val locationName = getCellStringValue(row.getCell(6))
                        val removalStatus = getCellStringValue(row.getCell(10))
                        
                        if (name.isNotEmpty()) {
                            // Find camper and location IDs for target year
                            val campers = dbHelper.getAllCampersAndInfrastructureForYear(targetYear)
                            val locations = dbHelper.getLocationsForYear(targetYear)
                            
                            val camper = campers.find { it.name == camperName }
                            val location = locations.find { it.name == locationName }
                            
                            if (camper != null && location != null) {
                                // Check if item exists
                                val existingItems = dbHelper.getAllItemsForYear(targetYear)
                                val exists = existingItems.any { 
                                    it.name == name && it.camperId == camper.id && it.locationId == location.id 
                                }
                                
                                if (!exists || !skipExisting) {
                                    val itemId = dbHelper.addItem(
                                        name = name,
                                        description = description,
                                        camperId = camper.id,
                                        locationId = location.id,
                                        year = targetYear
                                    )
                                    
                                    // Update removal status if needed
                                    if (removalStatus.isNotEmpty() && removalStatus != "active") {
                                        dbHelper.updateItem(
                                            id = itemId,
                                            name = name,
                                            description = description,
                                            camperId = camper.id,
                                            locationId = location.id,
                                            removalStatus = removalStatus
                                        )
                                    }
                                    
                                    result.itemsImported++
                                } else {
                                    result.itemsSkipped++
                                }
                            } else {
                                result.errors.add("Item row ${i + 1}: Could not find camper '$camperName' or location '$locationName'")
                            }
                        }
                    } catch (e: Exception) {
                        result.errors.add("Item row ${i + 1}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            result.errors.add("Item import error: ${e.message}")
        }
    }
    
    private fun getCellStringValue(cell: Cell?): String {
        return when (cell?.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> cell.numericCellValue.toLong().toString()
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> ""
        }
    }
    
    private fun getCellBooleanValue(cell: Cell?): Boolean {
        return when (cell?.cellType) {
            CellType.BOOLEAN -> cell.booleanCellValue
            CellType.STRING -> cell.stringCellValue.uppercase() in listOf("TRUE", "YES", "1")
            CellType.NUMERIC -> cell.numericCellValue != 0.0
            else -> false
        }
    }
}

data class ImportResult(
    var campersImported: Int = 0,
    var campersSkipped: Int = 0,
    var locationsImported: Int = 0,
    var locationsSkipped: Int = 0,
    var itemsImported: Int = 0,
    var itemsSkipped: Int = 0,
    var errors: MutableList<String> = mutableListOf(),
    var error: String? = null
) {
    val totalImported: Int
        get() = campersImported + locationsImported + itemsImported
    
    val totalSkipped: Int
        get() = campersSkipped + locationsSkipped + itemsSkipped
    
    val hasErrors: Boolean
        get() = error != null || errors.isNotEmpty()
}