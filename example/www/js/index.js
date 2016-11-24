var app = {
    initialize: function() {
        this.bindEvents();
    },
    bindEvents: function() {
        document.getElementById('button').addEventListener('click', this.buttonClicked, false);
    },
    buttonClicked: function() {
      	var cameraSelfieOpt = {
						quality: 50,
						destinationType: Camera.DestinationType.FILE_URI,
						sourceType: Camera.PictureSourceType.CAMERA,
						allowEdit: false,
						encodingType: Camera.EncodingType.JPEG,
						targetWidth: 100,
						targetHeight: 100,
						popoverOptions: CameraPopoverOptions,
						saveToPhotoAlbum: false,
						cameraDirection: 1,
						correctOrientation:true,
						toggleCamera: true
					};
					navigator.camera.getPicture(function(imageData) {
             console.log(imageData)
					}, function(err) {
							console.log("erro : " + err);
					}, cameraSelfieOpt);
    },
};

app.initialize();
