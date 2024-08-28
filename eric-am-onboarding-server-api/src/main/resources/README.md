# This README is dedicated for generating onboarding documentation

## Preconditions:

* ### Enabling local file access:
  If the ZIP file is unzipped on a local web server then it can be viewed using any browser. 
  To open the file directly from the local file system is possible only using Chrome or Firefox browsers. 
  This is because of security restrictions in cross origin access of local files.

  This issue can be mitigated by disabling this security setting when starting the browser:

    * Chrome:
      * Exit all Chrome browser instances if currently in use!
      * Select the Start Menu (the Windows icon) in the taskbar, or press the Windows key. Type cmd. 
      * Select Command Prompt from the list to open the Windows terminal window.
      * Type this command to start the Chrome browser allowing local file:// access:
      * "C:\Program Files\Google\Chrome\Application\chrome.exe" --allow-file-access-from-files 
  
    * Firefox:
      * Type about:config in the address bar to change configuration settings.
      * Search for the strict_origin_policy options and set the value to "false" allowing local file:// access.

## Steps:

* ### Creating documentation with one yaml file:
  * Open https://elib.internal.ericsson.com/ELIBSERV/rest2html
  * Put chosen file into input field and press "Create"
  * Download zip archive and decompress it
  * Open rest.html

* ### Creating documentation with one yaml file (with dependent file):
  * Only the main file is included in the ZIP file root folder.
  * All other files are placed into subfolders (example /commonObjects/commonObjects.yaml)
  * Compress files into one zip archive with respect of their structure (/api.yaml + /commonObjects/commonObjects.yaml)
  * Put chosen archive into input field and press "Create"
  * Download zip archive and decompress it
  * Open rest.html

## Notes
* For now rest2html can only generate one documentation at the time.
* Useful markdown documentation for writing descriptions: https://commonmark.org/help/
* It seems like that for now we can only generate one page at the time for any yaml file.
* Documentation for rest2html: 
https://calstore.internal.ericsson.com/elex?LI=EN/LZN7992020*&CL=EN/LZN7990031*&FN=14_00021-FCK10106Uen.*.html