"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Loader2Icon } from "lucide-react";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@/components/ui/field";
import { TokenStatusBadge } from "@/components/shared/status-badge";
import { useUpdateOlxAccount } from "@/hooks/use-olx-accounts";
import { toastMessages } from "@/lib/toast-messages";
import type { OlxAccount } from "@/types/olx";

const schema = z
  .object({
    username: z.string().min(2, "Korisničko ime mora imati najmanje 2 znaka."),
    password: z.string().optional(),
    passwordConfirm: z.string().optional(),
  })
  .refine((d) => !d.password || d.password === d.passwordConfirm, {
    message: "Lozinke se ne podudaraju.",
    path: ["passwordConfirm"],
  });

type FormValues = z.infer<typeof schema>;

export function EditProfileDialog({
  account,
  open,
  onOpenChange,
}: {
  account: OlxAccount | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const update = useUpdateOlxAccount();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: "", password: "", passwordConfirm: "" },
  });

  useEffect(() => {
    if (account && open) {
      reset({ username: account.username, password: "", passwordConfirm: "" });
    }
  }, [account, open, reset]);

  if (!account) return null;

  const onSubmit = handleSubmit((data) => {
    update.mutate(
      {
        id: account.id,
        username: data.username,
        password: data.password || undefined,
      },
      {
        onSuccess: () => {
          toast.success(toastMessages.updated);
          onOpenChange(false);
        },
        onError: (err) => {
          toast.error(err.message || toastMessages.error);
        },
      }
    );
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Uredi profil</DialogTitle>
          <DialogDescription>
            Ažurirajte korisničko ime ili promijenite lozinku profila.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={onSubmit} className="flex flex-col gap-4">
          <div className="flex items-center gap-3 rounded-md border bg-muted/30 p-3 text-sm">
            <div>
              <div className="text-xs text-muted-foreground">OLX user ID</div>
              <div className="font-medium">{account.olx_user_id ?? "—"}</div>
            </div>
            <div className="ml-auto">
              <TokenStatusBadge expiresAt={account.token_expires_at} />
            </div>
          </div>

          <Field data-invalid={!!errors.username || undefined}>
            <FieldLabel htmlFor="edit-username">Korisničko ime</FieldLabel>
            <Input
              id="edit-username"
              autoComplete="off"
              {...register("username")}
            />
            <FieldError
              errors={errors.username ? [errors.username] : undefined}
            />
          </Field>

          <Field data-invalid={!!errors.password || undefined}>
            <FieldLabel htmlFor="edit-password">Nova lozinka</FieldLabel>
            <Input
              id="edit-password"
              type="password"
              placeholder="Ostavite prazno za nepromijenjenu"
              autoComplete="new-password"
              {...register("password")}
            />
            <FieldDescription>
              Koristi se za automatsko osvježavanje OLX tokena.
            </FieldDescription>
          </Field>

          <Field data-invalid={!!errors.passwordConfirm || undefined}>
            <FieldLabel htmlFor="edit-password-confirm">
              Potvrdi lozinku
            </FieldLabel>
            <Input
              id="edit-password-confirm"
              type="password"
              autoComplete="new-password"
              {...register("passwordConfirm")}
            />
            <FieldError
              errors={
                errors.passwordConfirm ? [errors.passwordConfirm] : undefined
              }
            />
          </Field>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={update.isPending}
            >
              Otkaži
            </Button>
            <Button type="submit" disabled={update.isPending}>
              {update.isPending && (
                <Loader2Icon className="mr-2 size-4 animate-spin" />
              )}
              Sačuvaj
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
