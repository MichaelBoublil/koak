ld.lld  -melf_x86_64 -o $1 -dynamic-linker /lib64/ld-linux-x86-64.so.2 /usr/lib/crt1.o /usr/lib/crti.o  -lc $1.o /usr/lib/crtn.o `llvm-config --ldflags --system-libs --libs all`
chmod 777 $1
