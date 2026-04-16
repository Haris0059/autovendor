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
import { useRegister } from "@/hooks/use-auth";
import { toastMessages } from "@/lib/toast-messages";

const signupSchema = z.object({
  name: z.string().min(2, "Ime mora imati najmanje 2 znaka."),
  email: z.string().min(1, "Email je obavezan.").email("Nevažeća email adresa."),
  password: z.string().min(6, "Lozinka mora imati najmanje 6 znakova."),
});

type SignupInput = z.infer<typeof signupSchema>;

export function SignupForm({
  className,
  ...props
}: React.ComponentProps<"div">) {
  const router = useRouter();
  const register_ = useRegister();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SignupInput>({
    resolver: zodResolver(signupSchema),
    defaultValues: { name: "", email: "", password: "" },
  });

  const onSubmit = handleSubmit((data) => {
    register_.mutate(data, {
      onSuccess: () => {
        toast.success(toastMessages.registerSuccess);
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
            <h1 className="text-xl font-bold">Kreirajte AutoVendor račun</h1>
            <FieldDescription>
              Već imate račun?{" "}
              <Link href="/login" className="underline underline-offset-4">
                Prijavite se
              </Link>
            </FieldDescription>
          </div>

          <Field data-invalid={!!errors.name || undefined}>
            <FieldLabel htmlFor="name">Ime i prezime</FieldLabel>
            <Input
              id="name"
              type="text"
              placeholder="Haris Hodžić"
              autoComplete="name"
              {...register("name")}
            />
            <FieldError errors={errors.name ? [errors.name] : undefined} />
          </Field>

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
              placeholder="Najmanje 6 znakova"
              autoComplete="new-password"
              {...register("password")}
            />
            <FieldError errors={errors.password ? [errors.password] : undefined} />
          </Field>

          <Field>
            <Button type="submit" disabled={register_.isPending}>
              {register_.isPending && (
                <Loader2Icon className="mr-2 size-4 animate-spin" />
              )}
              Kreiraj račun
            </Button>
          </Field>
        </FieldGroup>
      </form>

      <FieldDescription className="px-6 text-center">
        Klikom na registraciju prihvatate{" "}
        <a href="#">Uslove korištenja</a> i <a href="#">Politiku privatnosti</a>.
      </FieldDescription>
    </div>
  );
}
