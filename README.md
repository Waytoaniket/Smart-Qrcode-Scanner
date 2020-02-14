# SmartQrScanner

Many apps have recently implemented a smart QR scanner much like the camera in IOS. Its a single camera view/fragment capable of firing UPI intents , saving contacts , opening web pages etc. 

Most of these apps rely on 3rd party sdks and apps to render the camera preview and scan the QR codes. Some of these SDK's have paid versions which cannot be altered and modified much. Thus a truely open source and modifiable scanner is required which is free and performs almost at par with paid alternatives.

This project aims to create a QR scanner which is completely open source, multi purpose , easily modifiable and minimises reliance on 3rd party libraries. 

We have used Gson and google zxing as the only 3rd party libraries for this project.

It also supports Upi intents and Bharat QR detection out of the box , which is a very popular p2p/p2m transaction technology in India.

## Usage
* Clone or Download the project
* Open the SmartCameraFragment 
* Observe the scanResult LiveData in the ScannerSharedViewModel
* Use the DeepLinkParser to perform appropriate actions

## Pre built features

* UPI Intents :- Opens the chooser with UPI apps when Qr code contains UPI / Bharat QR . 
* Web Pages : - Opens link in browser if QR code contains web based Urls
* Contacts :- Opens the contact book prefilled if the Qr code contains contacts
(Support is limited and doesnot follow any specific standard ).
* Wifi :- Connects to a particular wifi if its SSID and password is available in the QR code (Only works on a few devices)

## Additional features

Following are the features of the code base :-

* Uses the latest Camera2 api . Thus additional Camera2 featutes like autofocus , zoom , filters etc can be easily added
* Supports Bharat QR decryption out of the box.
* Toggle button to switch camera flash on and off is also available
* Supports loading image from gallery



