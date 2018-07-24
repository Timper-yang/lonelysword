//package com.timper.lonelysword
//
//import com.android.build.gradle.AppPlugin
//import com.android.build.gradle.LibraryPlugin
//import org.aspectj.bridge.IMessage
//import org.aspectj.bridge.MessageHandler
//import org.aspectj.tools.ajc.Main
//import org.gradle.api.Plugin
//import org.gradle.api.Project
//
//public class LonelyswordPlugin implements Plugin<Project> {
//
//  void apply(Project project) {
//    def hasApp = project.plugins.withType(AppPlugin)
//    def hasLib = project.plugins.withType(LibraryPlugin)
//    if (!hasApp && !hasLib) {
//      throw new IllegalStateException("'android' or 'android-library' plugin required.")
//    }
//
//    final def variants
//    if (hasApp) {
//      variants = project.android.applicationVariants
//    } else {
//      variants = project.android.libraryVariants
//    }
//    project.extensions.create('lonelysword', Properties)
//
//    project.dependencies {
//      api 'org.aspectj:aspectjrt:1.8.9'
//    }
//    final def log = project.logger
//    log.error "========================";
//    log.error "Aspectj切片开始编织Class!";
//    log.error "========================";
//    variants.all { variant ->
//
//      if (!variant.buildType.isDebuggable()) {
//        log.debug("Skipping non-debuggable build type '${variant.buildType.name}'.")
//        return;
//      } else if (!project.lonelysword.enabled) {
//        log.debug("lonelysword is not disabled.")
//        return;
//      }
//
//      def javaCompile = variant.javaCompile
//      javaCompile.doLast {
//        String[] args = ["-showWeaveInfo",
//                         "-1.8",
//                         "-inpath", javaCompile.destinationDir.toString(),
//                         "-aspectpath", javaCompile.classpath.asPath,
//                         "-d", javaCompile.destinationDir.toString(),
//                         "-classpath", javaCompile.classpath.asPath,
//                         "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)]
//        log.debug "ajc args: " + Arrays.toString(args)
//
//        MessageHandler handler = new MessageHandler(true);
//        new Main().run(args, handler);
//        for (IMessage message : handler.getMessages(null, true)) {
//          switch (message.getKind()) {
//            case IMessage.ABORT:
//            case IMessage.ERROR:
//            case IMessage.FAIL:
//              log.error message.message, message.thrown
//              break;
//            case IMessage.WARNING:
//              log.warn message.message, message.thrown
//              break;
//            case IMessage.INFO:
//              log.info message.message, message.thrown
//              break;
//            case IMessage.DEBUG:
//              log.debug message.message, message.thrown
//              break;
//          }
//        }
//      }
//    }
//  }
//}