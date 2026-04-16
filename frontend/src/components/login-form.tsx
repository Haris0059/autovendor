"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { StoreIcon, Loader2Icon } from "lucide-react";

import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { useLogin } from "@/hooks/use-auth";
import { toastMessages } from "@/lib/toast-messages";

const loginSchema = z.object({
  email: z.string().min(1, "Email je obavezan.").email("Nevažeća email adresa."),
  password: z.string().min(1, "Lozinka je obavezna."),
});

type LoginInput = z.infer<typeof loginSchema>;

export function LoginForm({
  className,
  ...props
}: React.ComponentProps<"div">) {
  const router = useRouter();
  const login = useLogin();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });

  const onSubmit = handleSubmit((data) => {
    login.mutate(data, {
      onSuccess: () => {
        toast.success(toastMessages.loginSuccess);
        router.push("/dashboard");
      },
      onError: (err) => {
        toast.error(err.message || toastMessages.error);
      },
    });
  });

  return (
    <div className={cn("flex flex-col gap-6", className)} {...props}>
      <form onSubmit={onSubmit}>
        <FieldGroup>
          <div className="flex flex-col items-center gap-2 text-center">
            <div className="flex size-10 items-center justify-center rounded-md bg-primary/10">
              <StoreIcon className="size-6 text-primary" />
            </div>
            <h1 className="text-xl font-bold">Prijava na AutoVendor</h1>
            <FieldDescription>
              Nemate račun?{" "}
              <Link href="/register" className="underline underline-offset-4">
                Registrujte se
              </Link>
            </FieldDescription>
          </div>

          <Field data-invalid={!!errors.email || undefined}>
            <FieldLabel htmlFor="email">Email</FieldLabel>
            <Input
              id="email"
              type="email"
              placeholder="vi@primjer.ba"
              autoComplete="email"
              {...register("email")}
            />
            <FieldError errors={errors.email ? [errors.email] : undefined} />
          </Field>

          <Field data-invalid={!!errors.password || undefined}>
            <FieldLabel htmlFor="password">Lozinka</FieldLabel>
            <Input
              id="password"
              type="password"
              placeholder="••••••••"
              autoComplete="current-password"
              {...register("password")}
            />
            <FieldError errors={errors.password ? [errors.password] : undefined} />
          </Field>

          <Field>
            <Button type="submit" disabled={login.isPending}>
              {login.isPending && <Loader2Icon className="mr-2 size-4 animate-spin" />}
              Prijavi se
            </Button>
          </Field>
        </FieldGroup>
      </form>

      <FieldDescription className="px-6 text-center">
        Klikom na prijavu prihvatate{" "}
        <a href="#">Uslove korištenja</a> i <a href="#">Politiku privatnosti</a>.
      </FieldDescription>
    </div>
  );
}
