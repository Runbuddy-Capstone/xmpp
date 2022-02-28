import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:neat_periodic_task/neat_periodic_task.dart';
import 'package:intl/intl.dart';

void main() => runApp(const MyApp());


class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(

        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({Key? key, required this.title}) : super(key: key);

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

void printInfo(String text) {
  print('\x1B[33m$text\x1B[0m');
}

String getDebugDateFormat(final DateTime dt) {
  return DateFormat('hh:mm:ss a').format(dt);
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel('runbuddy/xmpp');
  NeatPeriodicTaskScheduler? pubTimer;
  DateTime timeOfLastRecv = DateTime.now();
  // Time interval to publish. Two minutes by default.
  int timeIntervalMs = 1000 * 60 * 2;
  String message = "No message yet.....";

  @override
  void initState() {
    getMessageFromXMPP().then((String message) {
      setState(() {
        this.message = message;
      });
    });

    pubTimer = createPubSubTimer();
    super.initState();
  }

  NeatPeriodicTaskScheduler createPubSubTimer() {
    // Stop the timer and null it so the garbage collector destroys it.
    if(pubTimer != null) {
      pubTimer!.stop();
      pubTimer = null;
    }

    NeatPeriodicTaskScheduler newTimer = NeatPeriodicTaskScheduler(
        task: () async {
          printInfo("Pubbing...");

          setState(() {
            getMessageFromXMPP().then((String message) {
              this.message = message;
            });
            timeOfLastRecv = DateTime.now();
          });

          printInfo("Done pubbing.");
        },
      timeout: Duration(milliseconds: timeIntervalMs * 2),
      name: 'pubsubber',
      minCycle: Duration(milliseconds: timeIntervalMs ~/ 2 - 1),
      interval: Duration(milliseconds: timeIntervalMs),
    );

    newTimer.start();
    return newTimer;
  }

  Future<String> getMessageFromXMPP() async {
    String value = "";

    try {
      value = await platform.invokeMethod('getMessage');
    } catch(e) {
      print(e);
    }

    return value;
  }
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text(
              "Time of last recv is ${getDebugDateFormat(timeOfLastRecv)}",
            ),
            Text(
              message,
            ),
          ],
        ),
      ),
    );
  }
}
