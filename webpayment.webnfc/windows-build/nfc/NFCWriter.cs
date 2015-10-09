using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using Windows.Networking.Proximity;


namespace NFCWriter
{
    class NFCWriter
    {
        static void Main(string[] args)
        {
            ProximityDevice device = ProximityDevice.GetDefault();

            if (device != null)
            {
                long messageId =
      //            device.PublishMessage("Windows.message", "yeh");
                device.PublishUriMessage(new Uri(args[0])); //Reuse messageId to unpublish that message.
                Thread.Sleep(Timeout.Infinite);
            }
        }
    }
}
