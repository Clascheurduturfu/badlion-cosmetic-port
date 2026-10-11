// Pause only Badlion Java children long enough to attach the class observer.
const kernel = Process.getModuleByName('KernelBase.dll');
const resumeThread = new NativeFunction(kernel.getExportByName('ResumeThread'), 'uint32', ['pointer']);
const createEvent = new NativeFunction(kernel.getExportByName('CreateEventW'), 'pointer', ['pointer', 'int', 'int', 'pointer']);
const setEvent = new NativeFunction(kernel.getExportByName('SetEvent'), 'int', ['pointer']);
const waitForEvent = new NativeFunction(kernel.getExportByName('WaitForSingleObject'), 'uint32', ['pointer', 'uint32']);
const closeHandle = new NativeFunction(kernel.getExportByName('CloseHandle'), 'int', ['pointer']);
Interceptor.attach(kernel.getExportByName('CreateProcessW'), {
    onEnter(args) {
        this.selected = false;
        try {
            const application = args[0].isNull() ? '' : args[0].readUtf16String();
            const command = args[1].isNull() ? '' : args[1].readUtf16String();
            const identity = (application + ' ' + command).toLowerCase();
            if (!identity.includes('badlion') || !identity.includes('java')) return;
            this.flags = args[5].toUInt32();
            if ((this.flags & 4) !== 0) {
                send({event: 'already-suspended'});
                return;
            }
            this.selected = true;
            this.processInfo = args[9];
            args[5] = ptr(this.flags | 4); // CREATE_SUSPENDED
        } catch (error) {
            send({event: 'launcher-error', message: String(error)});
        }
    },
    onLeave(result) {
        if (!this.selected || result.toInt32() === 0) return;
        const threadHandle = this.processInfo.add(Process.pointerSize).readPointer();
        const pid = this.processInfo.add(Process.pointerSize * 2).readU32();
        const signal = createEvent(ptr(0), 1, 0, ptr(0));
        let closed = false;
        recv('resume-' + pid, function () { if (!closed) setEvent(signal); });
        send({event: 'java-child', pid});
        // Cooperative native waiting lets the acknowledgement callback run.
        // Timeout also resumes the client if the controller unexpectedly exits.
        const outcome = waitForEvent(signal, 15000);
        closed = true;
        closeHandle(signal);
        resumeThread(threadHandle);
        send({event: 'java-resumed', pid, timedOut: outcome === 258});
    }
});
send({event: 'launcher-ready'});
