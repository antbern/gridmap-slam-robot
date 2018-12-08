# Gridmap SLAM Robot

This is the source code and (eventually) documentation for my differential drive robot. I've written a backend in Java using OpenGL to visualize the LIDAR readings and control the robot. The lidar is home made using a pair of [VL53L1X](https://www.pololu.com/product/3415) laser ToF distance sensors mounted back to back. The robot is currently equipped with an Arduino Uno controlling the LIDAR stepper motor and the two DC motors for driving, as well as counting the motor encoder pulses and sending them back to the software running on the computer. The encoded count is then used as odometry control input to the SLAM algorithm.

In the future i intend to possibly move away from my own implementation of the SLAM algorithm to an existing one and maybe even to ROS.

[Here](https://photos.app.goo.gl/9LCzzc31TMR4rb7r5) is a link to a video of the robot in action. As you can see, the implementation kind of works, but still need much improvement to be really useable. My goal is to eventually have a robot that can wander around autonomously, creating a map of the environment!

## Useful links and resources on the subject

* [tinySLAM](https://openslam-org.github.io/tinyslam.html)

* [ROS: mrpt_icp_slam_2d](http://wiki.ros.org/mrpt_icp_slam_2d)

* [Field and Service Robotics: "beam endpoint model"](https://books.google.se/books?id=G09sCQAAQBAJ&pg=PA107&hl=sv&source=gbs_selected_pages&cad=3#v=onepage&q=beam%20endpoint%20model&f=false)

* [Probabilistic robotics / Sebastian Thrun, Wolfram Burgard, Dieter Fox]() - an excelent book on the subject.

* YouTube: [SLAM Course - WS13/14](https://www.youtube.com/playlist?list=PLgnQpQtFTOGQrZ4O5QzbIHgl3b1JHimN_) - great video recordings of a course on robotics and SLAM. Held by [Cyrill Stachniss](https://www.youtube.com/channel/UCi1TC2fLRvgBQNe-T4dp8Eg) at the University of Freiburg, Germany and contains a lot of references to the book above.

* YouTube: [SLAM Lectures](https://www.youtube.com/playlist?list=PLpUPoM7Rgzi_7YWn14Va2FODh7LzADBSm) - another series of videos explaining how to implement a "simple" land mark based SLAM algorithm in python by [Claus Brenner](https://www.youtube.com/channel/UCQoNsqW4v8uvrpWxnIabStg).