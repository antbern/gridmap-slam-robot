/*
    This library was taken from https://github.com/hideakitai/TFmini,
    and modified to add support for firmware version 16X of the TFMini (changed Short and added Medium distance mode constants)

    Original MIT License:

    Copyright (c) 2018 Hideaki Tai

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
*/

#pragma once


#ifndef TFMINI_H
#define TFMINI_H

class TFmini
{
public:

    static const uint32_t DEFAULT_BAUDRATE = 115200;

    enum class OutputDataFormat { Standard = 0x01, Pixhawk = 0x04 };
    enum class OutputDataUnit { MM = 0x00, CM = 0x01 };
    enum class DetectionPattern { Auto = 0x00, Fixed = 0x01 };
    enum class DistanceMode { Short = 0x00, Meduim = 0x03, Long = 0x07 };
    enum class TriggerSource { Internal = 0x01, External = 0x00 };
    enum class Baudrate
    {
        BAUD_9600, BAUD_14400, BAUD_19200, BAUD_38400, BAUD_56000, BAUD_57600, BAUD_115200,
        BAUD_128000, BAUD_230400, BAUD_256000, BAUD_460800, BAUD_500000, BAUD_512000
    };

    void attach(Stream& s) { stream_ = &s; }

    bool available()
    {
        if (!stream_) return false;

        update();
        if (b_available_)
        {
            b_available_ = false;
            return true;
        }
        else
            return false;
    }

    uint16_t getDistance() const { return packet_.distance.i; }
    uint16_t getStrength() const { return packet_.strength.i; }
    uint8_t getIntegrationTime() const { return packet_.int_time; }

    // default : Standard
    void setOutputDataFormat(const OutputDataFormat fmt)
    {
        format_ = fmt;
        configBegin();
        sendHeader();
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)fmt);
        stream_->write((uint8_t)0x06);
        configEnd();
    }

    // default : 10ms
    void setOutputDataPeriod(const uint16_t ms)
    {
        configBegin();
        sendHeader();
        stream_->write((uint8_t)((ms >> 8) & 0x00FF));
        stream_->write((uint8_t)((ms >> 0) & 0x00FF));
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x07);
        configEnd();
    }

    // default : cm
    void setOutputDataUnit(const OutputDataUnit unit)
    {
        configBegin();
        sendHeader();
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)unit);
        stream_->write((uint8_t)0x1A);
        configEnd();
    }

    // default : Auto
    void setDetectionPattern(const DetectionPattern pttr)
    {
        configBegin();
        sendHeader();
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)pttr);
        stream_->write((uint8_t)0x14);
        configEnd();
    }

    // usable when detection pattern is Fixed
    void setDistanceMode(const DistanceMode mode)
    {
        configBegin();
        sendHeader();
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)mode);
        stream_->write((uint8_t)0x11);
        configEnd();
    }

    // default : 12m
    void setRangeLimit(uint16_t mm)
    {
        configBegin();
        sendHeader();
        stream_->write((uint8_t)((mm >> 8) & 0x00FF));
        stream_->write((uint8_t)((mm >> 0) & 0x00FF));
        stream_->write((uint8_t)0x01);
        stream_->write((uint8_t)0x19);
        configEnd();
    }

    void disableRangeLimit()
    {
        configBegin();
        sendHeader();
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x19);
        configEnd();
    }

    // default : low = 20(DEC), high & cm is undefined
    void setSignalStrengthThreshold(uint8_t low, uint16_t high, uint8_t cm)
    {
        configBegin();
        // lower limit
        sendHeader();
        stream_->write((uint8_t)low);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x20);
        // upper limit
        sendHeader();
        stream_->write((uint8_t)((high >> 8) & 0x00FF));
        stream_->write((uint8_t)((high >> 0) & 0x00FF));
        stream_->write((uint8_t)cm);
        stream_->write((uint8_t)0x21);
        configEnd();
    }

    // default : 115200 (0x06)
    void setBaudRate(Baudrate baud)
    {
        configBegin();
        sendHeader();
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)baud);
        stream_->write((uint8_t)0x08);
        configEnd();
    }

    // default : Internal (100Hz)
    void setTriggerSource(const TriggerSource trigger)
    {
        configBegin();
        sendHeader();
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)trigger);
        stream_->write((uint8_t)0x40);
        configEnd();
    }

    // reset all settings
    void resetSettings()
    {
        configBegin();
        sendHeader();
        stream_->write((uint8_t)0xFF);
        stream_->write((uint8_t)0xFF);
        stream_->write((uint8_t)0xFF);
        stream_->write((uint8_t)0xFF);
        configEnd();
    }

private:

    void sendHeader()
    {
        stream_->write((uint8_t)0x42);
        stream_->write((uint8_t)0x57);
        stream_->write((uint8_t)0x02);
        stream_->write((uint8_t)0x00);
    }

    void configBegin()
    {
        sendHeader();
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x01);
        stream_->write((uint8_t)0x02);
    }

    void configEnd()
    {
        sendHeader();
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x00);
        stream_->write((uint8_t)0x02);
    }

    void update()
    {
        while(stream_->available())
        {
            uint8_t data = (uint8_t)stream_->read();

            if (format_ == OutputDataFormat::Pixhawk)
            {
                Serial.println("Pixhawk Format NOT SUPPORTED YET");
                return;
            }

            if (state_ != State::CHECKSUM) buffer_.sum += data;

            switch(state_)
            {
                case State::HEAD_L:
                {
                    reset();
                    buffer_.sum = data;
                    if (data == RECV_FRAME_HEADER) state_ = State::HEAD_H;
                    break;
                }
                case State::HEAD_H:
                {
                    if (data == RECV_FRAME_HEADER) state_ = State::DIST_L;
                    else                           state_ = State::HEAD_L;
                    break;
                }
                case State::DIST_L:
                {
                    buffer_.distance.b[0] = data;
                    state_ = State::DIST_H;
                    break;
                }
                case State::DIST_H:
                {
                    buffer_.distance.b[1] = data;
                    state_ = State::STRENGTH_L;
                    break;
                }
                case State::STRENGTH_L:
                {
                    buffer_.strength.b[0] = data;
                    state_ = State::STRENGTH_H;
                    break;
                }
                case State::STRENGTH_H:
                {
                    buffer_.strength.b[1] = data;
                    state_ = State::INT_TIME;
                    break;
                }
                case State::INT_TIME:
                {
                    buffer_.int_time = data;
                    state_ = State::RESERVED;
                    break;
                }
                case State::RESERVED:
                {
                    state_ = State::CHECKSUM;
                    break;
                }
                case State::CHECKSUM:
                {
                    if (buffer_.sum == data)
                    {
                        packet_ = buffer_;
                        b_available_ = true;
                    }
                    else
                    {
                        b_available_ = false;
                    }
                    reset();
                    break;
                }
                default:
                {
                    reset();
                    break;
                }
            }
        }
    }

    void reset()
    {
        buffer_.clear();
        state_ = State::HEAD_L;
    }

    struct Packet
    {
        union { uint8_t b[2]; uint16_t i; } distance;
        union { uint8_t b[2]; uint16_t i; } strength;
        uint8_t int_time;
        uint8_t sum;

        void clear() { distance.i = strength.i = int_time = sum = 0; }
    };

    enum class State
    {
        HEAD_L, HEAD_H, DIST_L, DIST_H, STRENGTH_L, STRENGTH_H, INT_TIME, RESERVED, CHECKSUM
    };

    static const uint8_t RECV_FRAME_HEADER = 0x59;

    Packet packet_;
    Packet buffer_;
    State state_;

    bool b_available_;
    Stream* stream_;

    OutputDataFormat format_ { OutputDataFormat::Standard };
};

#endif // TFMINI_H