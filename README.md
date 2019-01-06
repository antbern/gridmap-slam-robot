# Gridmap SLAM Robot

This is the source code and some documentation for my differential drive robot. I've written a backend in Java using OpenGL to visualize the LIDAR readings and control the robot. The lidar unit is home made using a single [TFMini](http://www.benewake.com/en/tfmini.html) sensor mounted on a rotating gear. The gear is driven by a stepper motor through a timing belt, and the signals are transfered using a cheap slip ring. Compared to the two [VL53L1X](https://www.pololu.com/product/3415) laser ToF distance sensors previously used, the TFMini sensor uses a strong IR LED (and no laser) and can measure up to 12 meters at a rate of 100 Hz.

The robot is currently equipped with an ESP32 controlling the LIDAR stepper motor and the two DC motors for driving, as well as counting the motor encoder pulses and sending them back to the software running on the computer. The encoder count is then used as odometry control input to the SLAM algorithm. Communication with the host machine is done through WiFi which, together with a LiPo battery, makes the robot completely wireless!

In the future i intend to possibly move away from my own implementation of the SLAM algorithm to an existing one and maybe even to ROS.

[Here](https://photos.app.goo.gl/9LCzzc31TMR4rb7r5) is a link to an older video of the robot in action. As you can see, the implementation kind of works, but still need much improvement to be really useable. My goal is to eventually have a robot that can wander around autonomously, creating a map of the environment!

## Useful links and resources on the subject

### SLAM

* [tinySLAM](https://openslam-org.github.io/tinyslam.html)

* [ROS: mrpt_icp_slam_2d](http://wiki.ros.org/mrpt_icp_slam_2d)

* [Field and Service Robotics: "beam endpoint model"](https://books.google.se/books?id=G09sCQAAQBAJ&pg=PA107&hl=sv&source=gbs_selected_pages&cad=3#v=onepage&q=beam%20endpoint%20model&f=false)

* [Probabilistic robotics / Sebastian Thrun, Wolfram Burgard, Dieter Fox]() - an excelent book on the subject.

* YouTube: [SLAM Course - WS13/14](https://www.youtube.com/playlist?list=PLgnQpQtFTOGQrZ4O5QzbIHgl3b1JHimN_) - great video recordings of a course on robotics and SLAM. Held by [Cyrill Stachniss](https://www.youtube.com/channel/UCi1TC2fLRvgBQNe-T4dp8Eg) at the University of Freiburg, Germany and contains a lot of references to the book above.

* YouTube: [SLAM Lectures](https://www.youtube.com/playlist?list=PLpUPoM7Rgzi_7YWn14Va2FODh7LzADBSm) - another series of videos explaining how to implement a "simple" land mark based SLAM algorithm in python by [Claus Brenner](https://www.youtube.com/channel/UCQoNsqW4v8uvrpWxnIabStg).

### ESP32

* [ESP32 Arduino Core](https://github.com/espressif/arduino-esp32)
* [Static IP](https://randomnerdtutorials.com/esp32-static-fixed-ip-address-arduino-ide/)
* [Soft AP](https://randomnerdtutorials.com/esp32-access-point-ap-web-server/)
* [Board Pinout (using 30 GPIO version)](https://randomnerdtutorials.com/getting-started-with-esp32/)
* [PWM](https://randomnerdtutorials.com/esp32-pwm-arduino-ide/)
* [ESP32 Exception decoder](https://github.com/me-no-dev/EspExceptionDecoder])
* [ESP-IDF Programming Guide about FreeRTOS](https://docs.espressif.com/projects/esp-idf/en/latest/api-reference/system/freertos.html)
