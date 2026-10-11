// Observe transformed class bytes without modifying class data or return values.
const hooked = new Set();
let observed = 0;
let exported = 0;

function relevant(name) {
    return name === null || name.startsWith('net/badlion/') ||
        name.startsWith('net/minecraft/') || /^[A-Za-z]{1,5}$/.test(name);
}

function emitClass(name, address, length, source) {
    if (length < 100 || length > 8 * 1024 * 1024 || address.isNull()) return;
    try {
        const bytes = address.readByteArray(length);
        const header = new Uint8Array(bytes, 0, 4);
        if (header[0] !== 0xca || header[1] !== 0xfe ||
            header[2] !== 0xba || header[3] !== 0xbe) return;
        exported++;
        send({event: 'class', name, length, source}, bytes);
    } catch (error) {
        send({event: 'capture-error', message: String(error)});
    }
}

Process.attachModuleObserver({
    onAdded(module) {
        if (module.name.toLowerCase() !== 'badlion_js.dll') return;
        const callback = module.findExportByName('Callback_ClassFileLoadHook');
        if (callback === null || hooked.has(callback.toString())) return;
        hooked.add(callback.toString());
        Interceptor.attach(callback, {
            onEnter(args) {
                observed++;
                try {
                    this.name = args[4].isNull() ? null : args[4].readUtf8String();
                    this.selected = relevant(this.name);
                    if (!this.selected) return;
                    this.inputLength = args[6].toInt32();
                    this.input = args[7];
                    this.outputLength = args[8];
                    this.output = args[9];
                } catch (_) { this.selected = false; }
            },
            onLeave() {
                if (!this.selected) return;
                try {
                    const length = this.outputLength.readS32();
                    const address = this.output.readPointer();
                    if (!address.isNull() && length > 0) {
                        emitClass(this.name, address, length, 'ClassFileLoadHook-output');
                    } else {
                        emitClass(this.name, this.input, this.inputLength, 'ClassFileLoadHook-input');
                    }
                } catch (error) {
                    send({event: 'capture-error', message: String(error)});
                }
            }
        });
        send({event: 'hook-ready', module: module.name, callback: callback.toString()});
    }
});
rpc.exports = { stats() { return {observed, exported, hooks: hooked.size}; } };
send({event: 'observer-ready'});
