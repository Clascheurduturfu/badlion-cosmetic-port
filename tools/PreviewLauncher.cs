using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Web.Script.Serialization;

// Small UI launcher so a preview can outlive the build terminal session.
internal static class PreviewLauncher
{
    public sealed class Request
    {
        public string java { get; set; }
        public string[] arguments { get; set; }
        public string cwd { get; set; }
    }
    [STAThread]
    private static void Main()
    {
        try
        {
            string folder = AppDomain.CurrentDomain.BaseDirectory;
            var json = new JavaScriptSerializer();
            var request = json.Deserialize<Request>(File.ReadAllText(Path.Combine(folder, "preview-command.json")));
            var arguments = request.arguments;
            var quoted = new List<string>();
            foreach (object value in arguments) quoted.Add("\"" + ((string)value).Replace("\"", "\\\"") + "\"");
            var info = new ProcessStartInfo(request.java, string.Join(" ", quoted));
            info.WorkingDirectory = request.cwd;
            info.UseShellExecute = false;
            info.CreateNoWindow = true;
            info.EnvironmentVariables["BCP_FORGE_AUDIT"] = "0";
            info.EnvironmentVariables["BCP_FORGE_PLAY"] = "1";
            var child = Process.Start(info);
            File.WriteAllText(Path.Combine(folder, "preview-pid.txt"), child.Id.ToString());
        }
        catch (Exception error)
        {
            File.WriteAllText(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "preview-launch-error.txt"), error.ToString());
        }
    }
}
