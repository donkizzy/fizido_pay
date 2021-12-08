#import "FizidoPayPlugin.h"
#if __has_include(<fizido_pay/fizido_pay-Swift.h>)
#import <fizido_pay/fizido_pay-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "fizido_pay-Swift.h"
#endif

@implementation FizidoPayPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFizidoPayPlugin registerWithRegistrar:registrar];
}
@end
