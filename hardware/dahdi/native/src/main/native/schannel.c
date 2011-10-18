#include <org_mobicents_ss7_hardware_dahdi_Channel.h>
#include <org_mobicents_ss7_hardware_dahdi_Selector.h>
#include <user.h>
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <sys/time.h>
#include <sys/poll.h>

#define ZAP_NUM_BUF 4

int fd[16];
struct pollfd fds[16];

int channel_count;

static int setnonblock_fd(int fd) {
    int res, flags;
    res = fcntl(fd, F_GETFL);
    
    if (res < 0) {
	return -1;
    }
    
    flags = res | O_NONBLOCK;
    
    res = fcntl(fd, F_SETFL, flags);
    if (res < 0) {
	return -1;
    }
    
    return 0;	    	
}

JNIEXPORT void JNICALL Java_org_mobicents_ss7_hardware_dahdi_Selector_doRegister (JNIEnv *env, jobject obj, jint fd) {
    int i = channel_count;
    channel_count = channel_count + 1;
    fds[i].fd = fd;
    fds[i].events = POLLIN|POLLPRI|POLLOUT;
}

JNIEXPORT void JNICALL Java_org_mobicents_ss7_hardware_dahdi_Selector_doUnregister (JNIEnv *env, jobject obj, jint fd) {
    struct pollfd temp[16];
    
    int i;
    int k;
    
    for (i = 0; i < channel_count; i++) {
	if (fds[i].fd != fd) {
	    temp[k++] = fds[i];
	}
    }
    
    channel_count = channel_count - 1;
    
    for (i = 0; i < channel_count; i++) {
	fds[i] = temp[i];
    }    
}

JNIEXPORT jint JNICALL Java_org_mobicents_ss7_hardware_dahdi_Selector_doPoll (JNIEnv *env, jobject obj, jintArray selected, jint flags, jint timeout) {
    int res;
    jint *elements = (*env)->GetIntArrayElements(env, selected, 0);
    
    int s;
    int k = 0;
    int lflags = flags;

    if ( (lflags & 0x1) ) {
	    s = POLLIN;
    } 
    if ( (lflags & 0x2 ) ) {
	    s = s | POLLOUT;
    } 
    res = poll(fds, channel_count, timeout);
    if (res > 0) {
		int i;
		for (i = 0; i < channel_count; i++) {
		    if (fds[i].revents & s) {
				elements[k++] = fds[i].fd;
		    }	    
		}
    }
    (*env)->ReleaseIntArrayElements(env, selected, elements, 0);
    return k;
}

JNIEXPORT jint JNICALL Java_org_mobicents_ss7_hardware_dahdi_Channel_openChannel
  (JNIEnv *env, jobject obj, jint zapid, jint ioBufferSize) {
    struct dahdi_bufferinfo bi;
    char devname[100];
    
    int fd;
    int res;
    
    sprintf(devname,"/dev/dahdi/%d",zapid );



    fd = open(devname, O_RDWR);
    if (fd < 0) {
        return -1;
    }


    bi.txbufpolicy = DAHDI_POLICY_IMMEDIATE;
    bi.rxbufpolicy = DAHDI_POLICY_IMMEDIATE;
    bi.numbufs = ZAP_NUM_BUF;
    bi.bufsize = ioBufferSize;
 
    ioctl(fd, DAHDI_SET_BUFINFO, &bi);
 
    res = setnonblock_fd(fd);
    if (res < 0) {
        return -1;
    }
    
    return fd;
}



/*
 * Class:     org_mobicents_media_server_impl_resource_zap_Schannel
 * Method:    read
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_org_mobicents_ss7_hardware_dahdi_Channel_readData
  (JNIEnv *env, jobject obj, jint fd, jbyteArray buff) {
    int res = 0;
    int count = 0;
    unsigned char buffer[1024];
    jbyte *elements = (*env)->GetByteArrayElements(env, buff, 0);
  
    for (;;) {
	//trying to read data
    	res = read(fd, buffer, 1024);
    	if(res > 0) {
    	    //copy data to java array
	    int k;
    	    for (k = 0; k < res; k++) {
    		elements[k + count] = buffer[k];
    	    }
    	    count = count + res;
    	    break;
	} else if( res == 0 ) {
    	    break;
	} else {
	    if (errno == EAGAIN || errno == EWOULDBLOCK) {
		break;
	    } else if (errno == EINTR) {
    	    /* try again */
	    } else if (errno == ELAST) {
    	    /* zap event */
	    } else {
		    /*some unexpected error*/
    		break;
	    }
	}
    }
    (*env)->ReleaseByteArrayElements(env, buff, elements, 0);
    return count;
}
    



/*
 * Class:     org_mobicents_media_server_impl_resource_zap_Schannel
 * Method:    write
 * Signature: ([BI)V
 */
JNIEXPORT void JNICALL Java_org_mobicents_ss7_hardware_dahdi_Channel_writeData
  (JNIEnv *env, jobject obj, jint fd, jbyteArray buff, jint length) {
    int res;
    int len = length;
    
    unsigned char buffer[len];
    jbyte *elements = (*env)->GetByteArrayElements(env, buff, 0);
    
    //copy data
    int i;
    for (i = 0; i < len; i++) {
	buffer[i] = elements[i];
    }
    
    //writting data
    for (;;) {
	res = write(fd, buffer, len);
	if (res == 0) {
	    break;
	} else if (res < 0) {
	    if (errno == EAGAIN || errno == EWOULDBLOCK) {
		break;
	    } else if (errno == EINTR) {
		//try again
	    } else if (errno == ELAST) {
		//fetch zap event
	    } else {
		//unexpected error
		break;
	    }
	} else {
	    break;
	}
    }
    (*env)->ReleaseByteArrayElements(env, buff, elements, 0);
}   
 
 

/*
 * Class:     org_mobicents_media_server_impl_resource_zap_Schannel
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_mobicents_ss7_hardware_dahdi_Channel_closeChannel (JNIEnv *env, jobject obj, jint fd) {
    close(fd);
}

