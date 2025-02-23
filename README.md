#**InLocker**

<!-- Generator: Adobe Illustrator 23.0.2, SVG Export Plug-In . SVG Version: 6.00 Build 0)  -->
<svg version="1.1" id="Layer_1" width="200" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
	 viewBox="0 0 106.6 144.6" style="enable-background:new 0 0 106.6 144.6;" xml:space="preserve">
</style>
<g>
	<g>
		<path class="st0" d="M13.7,10.2c0-1.9,0.7-3.6,2.2-5C17.3,3.8,19,3.1,21,3.1c1.9,0,3.6,0.7,5,2.1c1.4,1.4,2.1,3.1,2.1,5
			c0,2-0.7,3.7-2.1,5.1c-1.4,1.4-3.1,2.1-5,2.1c-2,0-3.7-0.7-5.1-2.1C14.4,13.9,13.7,12.2,13.7,10.2z"/>
		<path class="st0" d="M16.2,51.1H28V21.4H13.9v30C14.7,51.2,15.4,51.1,16.2,51.1z"/>
	</g>
	<path class="st0" d="M48.2,51.1v-3.6c0-7,5.7-12.8,12.8-12.8c7,0,12.7,5.7,12.8,12.7h0v3.6h10.2c1.1,0,2.1,0.2,3.1,0.4v-4h0
		c0,0,0,0,0,0v0h0c0-14.4-11.7-26-26-26c-4.7,0-9.1,1.2-12.9,3.4v-3.6H34.8v29.9H48.2z"/>
	<path class="st0" d="M87,100.9v31.6H73.7V80.3H87v2.8c2.2-0.9,5.1-1.7,8.2-2.1V62.4c0-5.1-3.5-9.5-8.2-10.8V74H73.7V51.1H48.2V74
		h-0.1v0H34.8V51.1H28v23.4H13.9V51.3c-5.1,1.1-9,5.6-9,11v67.7c0,6.2,5,11.3,11.3,11.3h67.7c6.2,0,11.3-5,11.3-11.3V94.5
		C90.9,95.5,87.5,97.6,87,100.9z M48.2,132.4H13.9V80.3h14.2l0,39.3h20.1L48.2,132.4z M58.5,132.4l-10.2-24.7l0,5.8H35V80.3h13.3
		l0,25.1l9-25.1h13.1L61,106.7l9.7,25.7H58.5z"/>
	<path class="st0" d="M95.2,81v13.4c2.5-0.6,5.3-0.9,8-0.9V80.3C100.8,80.3,97.9,80.6,95.2,81z"/>
</g>
</svg>

![Inlocker Logo](https://github.com/user-attachments/assets/99000a8e-3663-4078-8c5e-ceff9ebff249)


This app was developed on Kotlin for Android devices.

Inlocker can set passwords for every app the user has installed in their devices,
for when they are accessed by malicious user an overlay screen will block their access without the set password.
Passwords can be set individually or just one for all the apps, whichever the user desires.

<img src="https://github.com/user-attachments/assets/a18d1081-114f-4cee-bbbe-729764cd916e" alt="App List" width="300"/>

When the malicious user inputs a wrong password, the camera will take a frontal picture and send it
to the email address previously selected by the user, along with the device location in coordinates.

The app also gives the option of setting the app as a device administrator, blocking uninstalling
if the malicious user tries to uninstall to remove the overlaying effect.

<img src="https://github.com/user-attachments/assets/d8b3ebfd-e0f9-431f-b4f4-cc52b0c0c2e7" alt="Critical Settings" width="300"/>

obs: Email services won't work right now because it is still in testing and developing. You might want to use your own Gmail API to send emails to your chosen address.

This app asks for some permissions which the user may find invasive but after specific android updates, apps must ask the user for permissions
such as Accessibility and Display Pop-up Overlays to function as intended. 

<img src="https://github.com/user-attachments/assets/fc34eecf-9e9f-4b19-9aef-55ea8590e600" alt="accessibility" width="300"/>

<img src="https://github.com/user-attachments/assets/5d9e8362-061b-4b88-8b34-4272ccba56cf" alt="accessibility2" width="300"/>

<img src="https://github.com/user-attachments/assets/cf2d8d9a-6310-4fc7-beda-86df0c2e45d8" alt="battery1" width="300"/>

<img src="https://github.com/user-attachments/assets/d50c8759-8d30-464f-899a-911e97c173ba" alt="permissions" width="300"/>

<img src="https://github.com/user-attachments/assets/1d87a073-e291-463a-b537-36e6031be812" alt="overlay" width="300"/>






Don't worry, they can be removed whenever the user wishes. When InLocker is installed, enabled and with the passwords set, the device Settings menu will be protected by the password the user has set.
