//
//  RecognitionViewController.m
//  ClarifaiApiDemo
//

#import "RecognitionViewController.h"
#import "ClarifaiApiDemo-Swift.h"
#import "ClarifaiClient.h"
#import "DMActivityInstagram.h"


/**
 * This view controller performs recognition using the Clarifai API. This code is not run by
 * default (the Swift version is). See the README for instructions on using Objective-C.
 */
@interface RecognitionViewController () <UINavigationControllerDelegate, UIImagePickerControllerDelegate>
@property (weak, nonatomic) IBOutlet UIImageView *imageView;
@property (weak, nonatomic) IBOutlet UITextField *captionField;
@property (weak, nonatomic) IBOutlet UIButton *addButton;
@property (weak, nonatomic) IBOutlet UITextView *textView;
@property (weak, nonatomic) IBOutlet UIImageView *bg;
@property (weak, nonatomic) IBOutlet UIButton *button;
@property (weak, nonatomic) IBOutlet UIButton *takePic;
@property (weak, nonatomic) IBOutlet UIButton *returnButton;
@property (strong, nonatomic) ClarifaiClient *client;
@end


@implementation RecognitionViewController

- (ClarifaiClient *)client {
    if (!_client) {
        _client = [[ClarifaiClient alloc] initWithAppID:[Credentials clientID] appSecret:[Credentials clientSecret]];
    }
    return _client;
}

- (IBAction)takePic:(id)sender {
    
        UIImagePickerController *picker = [[UIImagePickerController alloc] init];
        picker.delegate = self;
        picker.allowsEditing = YES;
        picker.sourceType = UIImagePickerControllerSourceTypeCamera;
        
        [self presentViewController:picker animated:YES completion:NULL];
        
    

    
}

- (void)viewDidLoad {
    [super viewDidLoad];
    _addButton.hidden = YES;
    _returnButton.hidden = YES;
    
    // Do any additional setup after loading the view.
}


- (IBAction)returnButtonPressed:(id)sender {
    UIImagePickerController *picker = [[UIImagePickerController alloc] init];
    picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    picker.allowsEditing = NO;
    picker.delegate = self;
    [self presentViewController:picker animated:YES completion:nil];
    
    
}
- (IBAction)buttonPressed:(id)sender {
    // Show a UIImagePickerController to let the user pick an image from their library.
    self.bg.hidden = YES;
    self.button.hidden = YES;
    self.takePic.hidden = YES;
    self.addButton.hidden = NO;
    self.returnButton.hidden = NO;
    UIImagePickerController *picker = [[UIImagePickerController alloc] init];
    picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    picker.allowsEditing = NO;
    picker.delegate = self;
    [self presentViewController:picker animated:YES completion:nil];
}


- (IBAction)addButtonPressed:(id)sender {
     
    
    self.captionField.text = @"grizz";
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info {
    [self dismissViewControllerAnimated:YES completion:nil];
    UIImage *image = info[UIImagePickerControllerOriginalImage];
    if (image) {
        // The user picked an image. Send it to Clarifai for recognition.
        self.imageView.image = image;
        self.textView.text = @"Recognizing...";
        self.button.enabled = NO;
        [self recognizeImage:image];
    }
}

- (IBAction)postButton:(id)sender {
    DMActivityInstagram *instagramActivity = [[DMActivityInstagram alloc] init];
    NSString *shareText = self.textView.text;
    NSURL *shareURL = [NSURL URLWithString:@""];
    
    NSArray *activityItems = @[self.imageView.image, shareText, shareURL];
    UIActivityViewController *activityController = [[UIActivityViewController alloc] initWithActivityItems:activityItems applicationActivities:@[instagramActivity]];
    [self presentViewController:activityController animated:YES completion:nil];
    }


- (void)recognizeImage:(UIImage *)image {
    // Scale down the image. This step is optional. However, sending large images over the
    // network is slow and does not significantly improve recognition performance.
    CGSize size = CGSizeMake(320, 320 * image.size.height / image.size.width);
    UIGraphicsBeginImageContext(size);
    [image drawInRect:CGRectMake(0, 0, size.width, size.height)];
    UIImage *scaledImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    // Encode as a JPEG.
    NSData *jpeg = UIImageJPEGRepresentation(scaledImage, 0.9);

    // Send the JPEG to Clarifai for standard image tagging.
    [self.client recognizeJpegs:@[jpeg] completion:^(NSArray *results, NSError *error) {
        // Handle the response from Clarifai. This happens asynchronously.
        if (error) {
            NSLog(@"Error: %@", error);
            self.textView.text = @"Sorry, there was an error recognizing the image.";
        } else {
           
            ClarifaiResult *result = results.firstObject;
            
            self.textView.text = [NSString stringWithFormat:@"%@", [result.tags componentsJoinedByString:@"    #"]];
            NSString* a = self.textView.text;
            NSString* b = @"#";
            NSString* final = [b stringByAppendingString:a];
            NSString* grizzhash = @" #DevelopTheFuture";
            NSString* trueFinal = [final stringByAppendingString:grizzhash];
            self.textView.text = trueFinal;
            
            
        }
        self.button.enabled = YES;
    }];
}

@end
