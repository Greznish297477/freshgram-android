#include <stdbool.h>

#ifdef NDEBUG
#define LOG_DISABLED
#endif
// These constants bind the app to the certificate it was built with.
// They must be regenerated whenever the signing key changes, otherwise
// the app kills itself on startup (see check_signature in colorado.cpp).
#define PACKAGE_NAME "app.freshgram.android"_iobfs.c_str()
#define CERT_HASH 0xcc88ad67
#define CERT_SIZE 0x57c

#ifdef __cplusplus
extern "C" {
#endif

bool check_signature();

#ifdef __cplusplus
}
#endif