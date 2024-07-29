function doGet(e) {
  // Replace with your spreadsheet ID
  const spreadsheetId = '1pPGHA7sPgeC-S2YCTSrOyXX71JgI8BJ5g4FIEA6F4wU';
  const sheet = SpreadsheetApp.openById(spreadsheetId).getActiveSheet();
 var data = sheet.getDataRange().getValues();
 //sheet.createTextFinder()

  // Get the headers
  var headers = data[0];

  // Initialize an object to hold the final JSON object
  var jsonObject= {};
  var jsonArray = [];

  // Loop through the rows of data (starting from row 2 to skip headers)
  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var rowObject = {};

    // Loop through each column in the row
    for (var j = 0; j < headers.length; j++) {
      rowObject[headers[j]] = row[j];
    }

    jsonArray.push(rowObject);
  }

    jsonObject["data"] = jsonArray;


  // Log the JSON object (for debugging purposes)
  Logger.log(jsonObject);

  // Return the JSON object (this line is optional, depends on your use case)
  const response =  JSON.stringify(jsonObject);
  return ContentService.createTextOutput(response).setMimeType(ContentService.MimeType.JSON);

}
