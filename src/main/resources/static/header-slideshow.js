$(document).ready(function() {
    // Set up the classes to change the background image.
    let imageClassArray = new Array("bee", "tractor", "meadow");
    let imageClassIndex = 1;
    
    // Get the container and the overlay.
    let headerContainer = $("#hero-container");
    let headerOverlay = $("#hero-container .overlay");
    
    if ((headerContainer !== null) && (headerOverlay !== null)) {
        // Start the 'animation' every 10s.
        setInterval(function() {
            // Fade the overlay in over two seconds.
            headerOverlay.fadeIn(2000, 'swing', function() {
                // Change the class for the element and set up the next
                // class for the next change.
                headerContainer.removeClass();
                headerContainer.addClass(imageClassArray[imageClassIndex]);
                imageClassIndex = (imageClassIndex + 1) % imageClassArray.length;
                
                // Wait 100ms before fading the overlay out.
                headerOverlay.delay(100).fadeOut(2000);                
            });            
        }, 
        10000);    
    }   
});
